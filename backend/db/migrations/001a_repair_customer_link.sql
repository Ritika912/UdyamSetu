-- =====================================================================
-- Repair 001a: link enquiries that were given customer_id = 0
-- =====================================================================
-- Context: the application's Hibernate ddl-auto=update ran against udyamsetu_db before
-- 001_introduce_customers.sql was applied. It added enquiries.customer_id as NOT NULL, so
-- existing rows received 0, and the foreign key was not created.
--
-- PREREQUISITES
--   1. Stop the Spring Boot application.
--   2. Take a backup of udyamsetu_db (a dump taken just before this repair is recommended).
--
-- SAFETY
--   * Updates only rows where customer_id = 0. No rows are deleted and no other column changes.
--   * The UPDATE runs in a transaction and is rolled back unless every row was linked.
--   * The key rule matches ContactNumberNormalizer.java and 001_introduce_customers.sql.
--
-- RUN:  mysql -uroot -p udyamsetu_db < db/migrations/001a_repair_customer_link.sql
-- =====================================================================

START TRANSACTION;

-- Step 1: rows to repair (expected: the enquiries that still hold customer_id = 0).
SELECT id, contact_number FROM enquiries WHERE customer_id = 0;

-- Step 2: link each one to the customer whose contact_key matches its normalized number.
UPDATE enquiries e
JOIN customers c
  ON c.contact_key = IF(
        REPLACE(REPLACE(e.contact_number, ' ', ''), '-', '') REGEXP '^\\+?91[6-9][0-9]{9}$',
        RIGHT(REPLACE(REPLACE(e.contact_number, ' ', ''), '-', ''), 10),
        REPLACE(REPLACE(e.contact_number, ' ', ''), '-', ''))
SET e.customer_id = c.id
WHERE e.customer_id = 0;

-- Step 3: verify. If any row is still 0, ROLLBACK instead of COMMIT.
SELECT COUNT(*) AS rows_still_zero FROM enquiries WHERE customer_id = 0;

COMMIT;

-- Step 4: add the foreign key. Fails if any customer_id does not reference an existing customer.
ALTER TABLE enquiries
    ADD CONSTRAINT fk_enquiries_customer FOREIGN KEY (customer_id) REFERENCES customers (id);
