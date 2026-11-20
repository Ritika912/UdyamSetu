-- =====================================================================
-- Migration 003: link enquiries to the service catalogue
-- =====================================================================
-- Adds enquiries.service_id, a nullable foreign key to services.id.
-- Existing enquiries are NOT changed: service_id stays NULL and requested_service keeps its text.
-- There is deliberately no backfill, because catalogue entries must never be created from free text.
--
-- PREREQUISITES (in this order)
--   1. Migrations 001, 001a and 002 have been applied to udyamsetu_db.
--      (002 creates the services table; this migration cannot run without it.)
--   2. A verified backup of udyamsetu_db has been taken.
--   3. The application is stopped while this runs.
--
-- RUN ONCE:  mysql -uroot -p udyamsetu_db < db/migrations/003_link_enquiries_to_services.sql
--
-- SAFETY: adds one nullable column and one foreign key. No row is changed.
-- Not transactional in MySQL; if a step fails, stop and restore from backup.
-- =====================================================================

ALTER TABLE enquiries
    ADD COLUMN service_id BIGINT NULL AFTER customer_id;

ALTER TABLE enquiries
    ADD CONSTRAINT fk_enquiries_service FOREIGN KEY (service_id) REFERENCES services (id);

-- Verification: every existing enquiry must still exist, and none may point at a service.
SELECT COUNT(*)                                   AS enquiries_total,
       SUM(service_id IS NOT NULL)                AS enquiries_with_service
FROM enquiries;

-- ---------------------------------------------------------------------
-- ROLLBACK (only if needed; run before any enquiry references a service):
--   ALTER TABLE enquiries DROP FOREIGN KEY fk_enquiries_service;
--   ALTER TABLE enquiries DROP COLUMN service_id;
-- ---------------------------------------------------------------------
