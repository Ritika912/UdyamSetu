-- =====================================================================
-- Migration 002: create the services (service catalogue) table
-- =====================================================================
-- Creates the catalogue table only. It does not touch enquiries; the enquiries.service_id
-- column is added by a later migration once the Enquiry relationship is implemented.
--
-- PREREQUISITES
--   1. Migration 001 and repair 001a have been applied to udyamsetu_db.
--   2. A verified backup of udyamsetu_db has been taken.
--   3. The application is stopped while this runs.
--
-- RUN ONCE:  mysql -uroot -p udyamsetu_db < db/migrations/002_create_services_table.sql
--
-- SAFETY: creates a new table only. No existing table or row is changed. Not transactional in MySQL.
-- Matches the JPA mapping in ServiceOffering.java.
-- =====================================================================

CREATE TABLE services (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    description TEXT          NULL,
    category    VARCHAR(100)  NULL,
    price       DECIMAL(12,2) NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_services_name UNIQUE (name),
    CONSTRAINT ck_services_price_non_negative CHECK (price IS NULL OR price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- Verification: expect 0 rows in services before any catalogue entries are created.
SELECT COUNT(*) AS services_total FROM services;
