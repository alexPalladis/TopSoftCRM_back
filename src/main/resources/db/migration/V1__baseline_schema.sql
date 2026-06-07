-- ============================================================
-- V1__baseline_schema.sql
--
-- Initial schema for TopSoft CRM.
-- This runs once on first deploy (empty database).
-- All subsequent schema changes go in V2__, V3__, etc.
--
-- Convention: never edit this file after it has been applied.
-- ============================================================

SET NAMES utf8mb4;
SET time_zone = '+02:00';

-- ── admin_users ─────────────────────────────────────────────
CREATE TABLE admin_users (
                             id              CHAR(8)      NOT NULL,
                             username        VARCHAR(100) NOT NULL UNIQUE,
                             password_hash   VARCHAR(255) NOT NULL,
                             PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed the one admin account (password: 311268, bcrypt-hashed)
INSERT INTO admin_users (id, username, password_hash)
VALUES ('00000001', 'TOPSOFT', '$2a$10$Jf3YQJinNVHHMJJp8.YTweWH3GFEjIGe5V8VLfTg9tIqJnLUJOQMy');

-- ── networks ────────────────────────────────────────────────
CREATE TABLE networks (
                          id                  CHAR(8)      NOT NULL,
                          afm                 VARCHAR(9)   NOT NULL UNIQUE,
                          eponymia            VARCHAR(255) NOT NULL,
                          nomimos_ekprosopos  VARCHAR(255),
                          epaggelma           VARCHAR(255) NOT NULL,
                          doy                 VARCHAR(100) NOT NULL,
                          address             VARCHAR(255) NOT NULL,
                          city                VARCHAR(100) NOT NULL,
                          tk                  CHAR(5)      NOT NULL,
                          phone_fixed         VARCHAR(20),
                          phone_mobile        VARCHAR(20)  NOT NULL,
                          email               VARCHAR(255) NOT NULL,
                          username            VARCHAR(100) NOT NULL UNIQUE,
                          password_hash       VARCHAR(255) NOT NULL,
                          active              BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          INDEX idx_networks_city (city),
                          INDEX idx_networks_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── dealers ─────────────────────────────────────────────────
CREATE TABLE dealers (
                         id                  CHAR(8)      NOT NULL,
                         afm                 VARCHAR(9)   NOT NULL UNIQUE,
                         eponymia            VARCHAR(255) NOT NULL,
                         nomimos_ekprosopos  VARCHAR(255),
                         epaggelma           VARCHAR(255) NOT NULL,
                         doy                 VARCHAR(100) NOT NULL,
                         address             VARCHAR(255) NOT NULL,
                         city                VARCHAR(100) NOT NULL,
                         tk                  CHAR(5)      NOT NULL,
                         phone_fixed         VARCHAR(20),
                         phone_mobile        VARCHAR(20)  NOT NULL,
                         email               VARCHAR(255) NOT NULL,
                         username            VARCHAR(100) NOT NULL UNIQUE,
                         password_hash       VARCHAR(255) NOT NULL,
                         network_id          CHAR(8)      NULL,
                         active              BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (id),
                         INDEX idx_dealers_network_id (network_id),
                         INDEX idx_dealers_city (city),
                         INDEX idx_dealers_active (active),
                         CONSTRAINT fk_dealers_network FOREIGN KEY (network_id)
                             REFERENCES networks(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── subdealers ──────────────────────────────────────────────
CREATE TABLE subdealers (
                            id                  CHAR(8)      NOT NULL,
                            afm                 VARCHAR(9)   NOT NULL UNIQUE,
                            eponymia            VARCHAR(255) NOT NULL,
                            nomimos_ekprosopos  VARCHAR(255),
                            epaggelma           VARCHAR(255) NOT NULL,
                            doy                 VARCHAR(100) NOT NULL,
                            address             VARCHAR(255) NOT NULL,
                            city                VARCHAR(100) NOT NULL,
                            tk                  CHAR(5)      NOT NULL,
                            phone_fixed         VARCHAR(20),
                            phone_mobile        VARCHAR(20)  NOT NULL,
                            email               VARCHAR(255) NOT NULL,
                            username            VARCHAR(100) NOT NULL UNIQUE,
                            password_hash       VARCHAR(255) NOT NULL,
                            dealer_id           CHAR(8)      NOT NULL,
                            active              BOOLEAN      NOT NULL DEFAULT TRUE,
                            created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            INDEX idx_subdealers_dealer_id (dealer_id),
                            INDEX idx_subdealers_city (city),
                            CONSTRAINT fk_subdealers_dealer FOREIGN KEY (dealer_id)
                                REFERENCES dealers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── customers ───────────────────────────────────────────────
CREATE TABLE customers (
                           id                  CHAR(8)      NOT NULL,
                           afm                 VARCHAR(9)   NOT NULL UNIQUE,
                           eponymia            VARCHAR(255) NOT NULL,
                           nomimos_ekprosopos  VARCHAR(255),
                           epaggelma           VARCHAR(255) NOT NULL,
                           doy                 VARCHAR(100) NOT NULL,
                           address             VARCHAR(255) NOT NULL,
                           city                VARCHAR(100) NOT NULL,
                           tk                  CHAR(5)      NOT NULL,
                           phone_fixed         VARCHAR(20),
                           phone_mobile        VARCHAR(20)  NOT NULL,
                           email               VARCHAR(255) NOT NULL,
                           active              BOOLEAN      NOT NULL DEFAULT TRUE,
                           dealer_id           CHAR(8)      NOT NULL,
                           subdealer_id        CHAR(8)      NULL,
                           source              VARCHAR(50)  NULL,         -- 'API' or 'MANUAL'
                           referral_code       VARCHAR(20)  NULL,
                           created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (id),
                           INDEX idx_customers_dealer_id (dealer_id),
                           INDEX idx_customers_subdealer_id (subdealer_id),
                           INDEX idx_customers_city (city),
                           INDEX idx_customers_active (active),
                           CONSTRAINT fk_customers_dealer    FOREIGN KEY (dealer_id)
                               REFERENCES dealers(id),
                           CONSTRAINT fk_customers_subdealer FOREIGN KEY (subdealer_id)
                               REFERENCES subdealers(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── customer_subscriptions ──────────────────────────────────
CREATE TABLE customer_subscriptions (
                                        id           BIGINT       NOT NULL AUTO_INCREMENT,
                                        customer_id  CHAR(8)      NOT NULL,
                                        product_id   INT          NOT NULL,   -- 1–8 fixed product list
                                        active       BOOLEAN      NOT NULL DEFAULT FALSE,
                                        expiry_date  DATE         NULL,       -- for DATE-type products
                                        quantity     INT          NULL,       -- for QUANTITY-type (SMS, email)
                                        cost         DECIMAL(10,2) NOT NULL DEFAULT 0,
                                        PRIMARY KEY (id),
                                        UNIQUE KEY uq_customer_product (customer_id, product_id),
                                        INDEX idx_subs_customer_id (customer_id),
                                        CONSTRAINT fk_subs_customer FOREIGN KEY (customer_id)
                                            REFERENCES customers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── referral_codes ──────────────────────────────────────────
CREATE TABLE referral_codes (
                                code         VARCHAR(20)  NOT NULL,
                                entity_type  VARCHAR(20)  NOT NULL,   -- DEALER or SUBDEALER
                                entity_id    CHAR(8)      NOT NULL,
                                active       BOOLEAN      NOT NULL DEFAULT TRUE,
                                created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (code),
                                INDEX idx_refcodes_entity_id (entity_id),
                                INDEX idx_refcodes_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── commissions (price catalog per entity) ──────────────────
CREATE TABLE commissions (
                             id           BIGINT        NOT NULL AUTO_INCREMENT,
                             entity_type  VARCHAR(20)   NOT NULL,   -- NETWORK or DEALER (global default uses entity_id='DEFAULT')
                             entity_id    CHAR(8)       NOT NULL,
                             product_id   INT           NOT NULL,
                             commission_pct DECIMAL(5,2) NOT NULL DEFAULT 0,
                             sale_price   DECIMAL(10,2) NOT NULL DEFAULT 0,
                             PRIMARY KEY (id),
                             UNIQUE KEY uq_commission (entity_type, entity_id, product_id),
                             INDEX idx_commissions_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── commission_history ──────────────────────────────────────
CREATE TABLE commission_history (
                                    id                      BIGINT        NOT NULL AUTO_INCREMENT,
                                    payment_date            DATE          NOT NULL,
                                    product_id              INT           NOT NULL,
                                    customer_id             CHAR(8)       NOT NULL,
                                    customer_afm            VARCHAR(9)    NOT NULL,
                                    customer_eponymia       VARCHAR(255)  NOT NULL,
                                    amount                  DECIMAL(10,2) NOT NULL,
                                    dealer_id               CHAR(8)       NULL,
                                    dealer_commission_pct   DECIMAL(5,2)  NULL,
                                    dealer_commission_amt   DECIMAL(10,2) NULL,
                                    network_id              CHAR(8)       NULL,
                                    network_commission_pct  DECIMAL(5,2)  NULL,
                                    network_commission_amt  DECIMAL(10,2) NULL,
                                    paid_dealer             BOOLEAN       NOT NULL DEFAULT FALSE,
                                    paid_network            BOOLEAN       NOT NULL DEFAULT FALSE,
                                    receipt                 VARCHAR(500)  NULL,
                                    external_ref            VARCHAR(255)  NULL,
                                    created_at              DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    PRIMARY KEY (id),
                                    INDEX idx_commhist_dealer_id (dealer_id),
                                    INDEX idx_commhist_network_id (network_id),
                                    INDEX idx_commhist_payment_date (payment_date),
                                    INDEX idx_commhist_customer_afm (customer_afm)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── requests ────────────────────────────────────────────────
CREATE TABLE requests (
                          id              BIGINT        NOT NULL AUTO_INCREMENT,
                          request_date    DATE          NOT NULL,
                          from_role       VARCHAR(20)   NOT NULL,
                          from_id         CHAR(8)       NOT NULL,
                          from_name       VARCHAR(255)  NOT NULL,
                          to_role         VARCHAR(20)   NOT NULL,
                          to_id           CHAR(8)       NOT NULL,
                          to_name         VARCHAR(255)  NOT NULL,
                          subject         VARCHAR(500)  NOT NULL,
                          body            TEXT          NOT NULL,
                          status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',  -- PENDING, COMPLETED
                          created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (id),
                          INDEX idx_requests_from_id (from_id),
                          INDEX idx_requests_to_id (to_id),
                          INDEX idx_requests_status (status),
                          INDEX idx_requests_date (request_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── audit_logs ──────────────────────────────────────────────
CREATE TABLE audit_logs (
                            id           BIGINT        NOT NULL AUTO_INCREMENT,
                            actor_id     CHAR(8)       NOT NULL,
                            actor_role   VARCHAR(20)   NOT NULL,
                            actor_name   VARCHAR(200)  NULL,
                            entity_type  VARCHAR(50)   NOT NULL,
                            entity_id    VARCHAR(50)   NULL,
                            entity_name  VARCHAR(300)  NULL,
                            action       VARCHAR(50)   NOT NULL,
                            description  VARCHAR(500)  NULL,
                            created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            INDEX idx_audit_actor_id (actor_id),
                            INDEX idx_audit_entity_type (entity_type),
                            INDEX idx_audit_entity_id (entity_id),
                            INDEX idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;