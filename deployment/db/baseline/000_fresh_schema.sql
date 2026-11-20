-- =====================================================================
-- Fresh production schema baseline (UdyamSetu)
-- =====================================================================
-- Creates exactly the three business tables for an EMPTY database: customers, services, enquiries.
-- Derived from the JPA entity mappings (Customer, ServiceOffering, Enquiry) and the Hibernate DDL verified
-- against MySQL 8.4 with ddl-auto=validate, under both the normal Connector/J settings and tinyInt1isBit=false.
--
-- Replaces migrations 001, 001a, 002 and 003 for a FRESH database. Those migrations are upgrade steps for the
-- existing development database only. Do not run them on a fresh database.
--
-- Contains no seed data and no business rows. Do not copy local or test data into the production database.
--
-- PREREQUISITES
--   1. An empty database created with utf8mb4 (the Compose MySQL service creates udyamsetu_prod empty).
--   2. Run as an administrator, not as the application account. The application account needs DML only.
--
-- RUN ONCE, against the EMPTY production database, by an administrator. Do not run it against any other database.
-- =====================================================================

-- ---------------------------------------------------------------------
-- customers
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
-- services (service catalogue; soft deactivation through is_active)
-- ---------------------------------------------------------------------
CREATE TABLE services (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    description TEXT          NULL,
    category    VARCHAR(100)  NULL,
    price       DECIMAL(12,2) NULL,
    is_active   BIT(1)        NOT NULL DEFAULT b'1',
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_services_name UNIQUE (name),
    CONSTRAINT ck_services_price_non_negative CHECK (price IS NULL OR price >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- enquiries (customer_id mandatory; service_id optional, for catalogue-linked enquiries only)
-- ENUM values match EnquirySource and EnquiryStatus exactly.
-- ---------------------------------------------------------------------
CREATE TABLE enquiries (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    customer_id       BIGINT        NOT NULL,
    service_id        BIGINT        NULL,
    customer_name     VARCHAR(100)  NOT NULL,
    contact_number    VARCHAR(20)   NOT NULL,
    source            ENUM('WEBSITE','PHONE','WHATSAPP','WALK_IN','SOCIAL_MEDIA','OTHER') NULL,
    requested_service VARCHAR(150)  NULL,
    requirements      TEXT          NULL,
    occasion          VARCHAR(100)  NULL,
    budget            DECIMAL(12,2) NULL,
    expected_date     DATE          NULL,
    follow_up_date    DATE          NULL,
    follow_up_time    TIME(0)       NULL,
    follow_up_notes   TEXT          NULL,
    status            ENUM('NEW','IN_PROGRESS','PENDING','CONVERTED','UNFULFILLED','LOST') NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_enquiries_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_enquiries_service  FOREIGN KEY (service_id)  REFERENCES services (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
