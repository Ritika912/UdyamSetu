-- =====================================================================
-- Migration 001: introduce customers and link existing enquiries
-- =====================================================================
-- PREREQUISITES (in this order)
--   1. Take a full backup of udyamsetu_db and confirm it is complete.
--   2. Stop the Spring Boot application. Hibernate (ddl-auto=update) must not start
--      against the database before this script has run.
--   3. Run Step 0 (pre-flight). Resolve any rows it returns before continuing.
--
-- RUN ONCE:  mysql -uroot -p udyamsetu_db < db/migrations/001_introduce_customers.sql
--
-- SAFETY
--   * No enquiry row is deleted or updated except its customer_id column.
--   * customer_name / contact_number on enquiries are kept as the per-enquiry snapshot.
--   * DDL is not transactional in MySQL; if a step fails, stop and restore from backup.
--
-- Key rule (must match ContactNumberNormalizer.java):
--   digits = number with spaces and hyphens removed (a leading '+' is kept)
--   Indian  : 10 digits 6-9xxxxxxxx, or 12 digits 91 + 6-9xxxxxxxx (with or without '+')
--             -> contact_key = the 10-digit mobile number
--   Other   : '+' followed by 7-15 digits, not starting with +91
--             -> contact_key = '+' followed by the digits
-- =====================================================================


-- ---------------------------------------------------------------------
-- Step 0: PRE-FLIGHT (read-only). Lists enquiries whose contact number is invalid under the new rules.
-- Expected: an empty result. If rows appear, fix or review them first; the backfill would otherwise
-- guess at their identity.
-- ---------------------------------------------------------------------
SELECT e.id, e.contact_number
FROM enquiries e
WHERE NOT (
    REPLACE(REPLACE(e.contact_number, ' ', ''), '-', '') REGEXP '^[6-9][0-9]{9}$'
    OR REPLACE(REPLACE(e.contact_number, ' ', ''), '-', '') REGEXP '^\\+?91[6-9][0-9]{9}$'
    OR (REPLACE(REPLACE(e.contact_number, ' ', ''), '-', '') REGEXP '^\\+[0-9]{7,15}$'
        AND REPLACE(REPLACE(e.contact_number, ' ', ''), '-', '') NOT LIKE '+91%')
);


-- ---------------------------------------------------------------------
-- Step 1: create the customers table (same definition Hibernate would generate).
-- ---------------------------------------------------------------------
CREATE TABLE customers (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    customer_name  VARCHAR(100) NOT NULL,
    contact_number VARCHAR(20)  NOT NULL,
    contact_key    VARCHAR(20)  NOT NULL,
    notes          TEXT         NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customers_contact_key UNIQUE (contact_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


-- ---------------------------------------------------------------------
-- Step 2: add the link column. Nullable for now so existing rows are accepted; Step 6 makes it NOT NULL.
-- ---------------------------------------------------------------------
ALTER TABLE enquiries
    ADD COLUMN customer_id BIGINT NULL AFTER id;

ALTER TABLE enquiries
    ADD CONSTRAINT fk_enquiries_customer FOREIGN KEY (customer_id) REFERENCES customers (id);


-- ---------------------------------------------------------------------
-- Step 3: compute the contact key for every enquiry that is not yet linked.
-- (Temporary table so the same key logic is used by both the insert and the update.)
-- ---------------------------------------------------------------------
CREATE TEMPORARY TABLE tmp_enquiry_keys AS
SELECT x.id,
       x.customer_name,
       x.contact_number,
       x.created_at,
       IF(x.digits REGEXP '^\\+?91[6-9][0-9]{9}$', RIGHT(x.digits, 10), x.digits) AS contact_key
FROM (
    SELECT e.id,
           e.customer_name,
           e.contact_number,
           e.created_at,
           REPLACE(REPLACE(e.contact_number, ' ', ''), '-', '') AS digits
    FROM enquiries e
    WHERE e.customer_id IS NULL
) x;


-- ---------------------------------------------------------------------
-- Step 4: create one customer per distinct contact key.
-- The earliest enquiry for each key supplies the customer's name and number.
-- Re-running this step does not create duplicates (NOT EXISTS guard).
-- ---------------------------------------------------------------------
INSERT INTO customers (customer_name, contact_number, contact_key, notes, created_at, updated_at)
SELECT ranked.customer_name,
       ranked.contact_number,
       ranked.contact_key,
       NULL,
       ranked.created_at,
       NOW(6)
FROM (
    SELECT k.customer_name,
           k.contact_number,
           k.contact_key,
           k.created_at,
           ROW_NUMBER() OVER (PARTITION BY k.contact_key ORDER BY k.created_at, k.id) AS rn
    FROM tmp_enquiry_keys k
) ranked
WHERE ranked.rn = 1
  AND NOT EXISTS (SELECT 1 FROM customers c WHERE c.contact_key = ranked.contact_key);


-- ---------------------------------------------------------------------
-- Step 5: link each unlinked enquiry to its customer, then drop the temporary table.
-- ---------------------------------------------------------------------
UPDATE enquiries e
JOIN tmp_enquiry_keys k ON k.id = e.id
JOIN customers c        ON c.contact_key = k.contact_key
SET e.customer_id = c.id
WHERE e.customer_id IS NULL;

DROP TEMPORARY TABLE tmp_enquiry_keys;


-- ---------------------------------------------------------------------
-- Step 6: VERIFY, then enforce NOT NULL.
-- Check: enquiries_total equals the row count recorded before the migration, and enquiries_unlinked is 0.
-- ---------------------------------------------------------------------
SELECT (SELECT COUNT(*) FROM enquiries)                         AS enquiries_total,
       (SELECT COUNT(*) FROM enquiries WHERE customer_id IS NULL) AS enquiries_unlinked,
       (SELECT COUNT(*) FROM customers)                         AS customers_total;

-- Fails with an error if any enquiry is still unlinked, which is the intended guard.
ALTER TABLE enquiries
    MODIFY customer_id BIGINT NOT NULL;


-- ---------------------------------------------------------------------
-- ROLLBACK (only if a failure requires it; run before any new customer-linked enquiries exist):
--   ALTER TABLE enquiries DROP FOREIGN KEY fk_enquiries_customer;
--   ALTER TABLE enquiries DROP COLUMN customer_id;
--   DROP TABLE customers;
-- ---------------------------------------------------------------------
