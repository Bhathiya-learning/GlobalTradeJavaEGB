-- ==============================================================================
-- GlobalTrade Logistics Corporation - Fully Normalized Database Schema (3NF / BCNF)
-- Module: Business Component Development II (BCD II)
-- DBMS: MySQL 8.0+ / MariaDB 10.5+ / PostgreSQL Compatible DDL
-- Standard: Strictly Normalized to 3rd Normal Form (3NF) & Boyce-Codd Normal Form (BCNF)
-- Description: Fully normalized enterprise database schema supporting multi-module EJB
--              architecture, zero transitive dependencies, reference lookup tables,
--              automated timer services, logistics audit trailing, customs trade compliance,
--              vendor evaluations, and JTA transaction coordination.
-- ==============================================================================

DROP DATABASE IF EXISTS globaltrade_db;
CREATE DATABASE globaltrade_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE globaltrade_db;

-- ------------------------------------------------------------------------------
-- 1. REFERENCE & LOOKUP MASTER TABLES (Normalization Level: 3NF / BCNF)
-- ------------------------------------------------------------------------------

-- 1.1 Genders Table
CREATE TABLE genders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.2 Roles Table (EJB Security Roles & JAAS Mapping)
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.3 Countries Master Table
CREATE TABLE countries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    iso_code_2 CHAR(2) NOT NULL UNIQUE,
    iso_code_3 CHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.4 Cities Master Table (FK -> countries)
CREATE TABLE cities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    country_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20),
    CONSTRAINT fk_cities_country FOREIGN KEY (country_id) REFERENCES countries(id),
    CONSTRAINT uk_city_country UNIQUE (name, country_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.5 Addresses Master Table (FK -> cities)
CREATE TABLE addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_id BIGINT NOT NULL,
    address_line1 VARCHAR(150) NOT NULL,
    address_line2 VARCHAR(150),
    CONSTRAINT fk_addresses_city FOREIGN KEY (city_id) REFERENCES cities(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.6 Shipping Carriers Master Table
CREATE TABLE carriers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrier_code VARCHAR(30) NOT NULL UNIQUE,
    company_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.7 Vendor Compliance Statuses Table
CREATE TABLE vendor_compliance_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.8 Shipment Statuses Table
CREATE TABLE shipment_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.9 Customs Statuses Table
CREATE TABLE customs_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.10 Harmonized System (HS) Tariff Codes Table
CREATE TABLE hs_tariff_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hs_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    default_duty_rate_pct DECIMAL(5,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.11 Audit Action Types Table
CREATE TABLE audit_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.12 Timer Statuses Table
CREATE TABLE timer_statuses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.13 System Severity Levels Table
CREATE TABLE severity_levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 1.14 Failure Scenarios Reference Table
CREATE TABLE failure_scenarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- 2. OPERATIONAL MASTER DATA TABLES (3NF / BCNF)
-- ------------------------------------------------------------------------------

-- 2.1 Vendors Table (FK -> countries, vendor_compliance_statuses)
CREATE TABLE vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(100) NOT NULL,
    country_id BIGINT NOT NULL,
    contact_email VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(30),
    rating DECIMAL(3,2) NOT NULL DEFAULT 5.00,
    compliance_status_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vendors_country FOREIGN KEY (country_id) REFERENCES countries(id),
    CONSTRAINT fk_vendors_compliance FOREIGN KEY (compliance_status_id) REFERENCES vendor_compliance_statuses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2.2 Warehouses Table (FK -> addresses)
CREATE TABLE warehouses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    warehouse_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    address_id BIGINT NOT NULL,
    total_capacity_sqm DECIMAL(10,2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_warehouses_address FOREIGN KEY (address_id) REFERENCES addresses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2.3 Users Table (FK -> genders, roles, vendors, warehouses)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- SHA-256 / BCrypt Hashed
    salt VARCHAR(64) DEFAULT NULL,   -- SecureRandom Hex Salt for Cryptographic Hashing
    mobile VARCHAR(20) NOT NULL,
    gender_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    vendor_id BIGINT DEFAULT NULL,
    warehouse_id BIGINT DEFAULT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_gender FOREIGN KEY (gender_id) REFERENCES genders(id),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_users_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE SET NULL,
    CONSTRAINT fk_users_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2.3.1 Dedicated Customers Profile Table (FK -> users)
CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(150),
    eori_number VARCHAR(100),
    tax_id VARCHAR(100),
    shipping_address VARCHAR(255),
    billing_address VARCHAR(255),
    credit_limit DECIMAL(12,2) DEFAULT 50000.00,
    avatar_url LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2.4 Product Categories Table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2.5 Inventory Items Table (FK -> categories, vendors, warehouses)
CREATE TABLE inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    unit_price DECIMAL(12,2) NOT NULL,
    stock_level INT NOT NULL DEFAULT 0,
    min_stock_level INT NOT NULL DEFAULT 10,
    reorder_quantity INT NOT NULL DEFAULT 50,
    category_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_items_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id),
    CONSTRAINT fk_items_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- 3. TRANSACTIONAL & LOGISTICS TABLES (3NF / BCNF)
-- ------------------------------------------------------------------------------

-- 3.1 Shipments Table (FK -> warehouses, addresses, carriers, shipment_statuses, users, customers)
CREATE TABLE shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tracking_number VARCHAR(50) NOT NULL UNIQUE,
    origin_warehouse_id BIGINT NOT NULL,
    destination_address_id BIGINT NOT NULL,
    carrier_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    customer_id BIGINT DEFAULT NULL,
    dispatch_date TIMESTAMP NULL DEFAULT NULL,
    estimated_delivery TIMESTAMP NULL DEFAULT NULL,
    actual_delivery TIMESTAMP NULL DEFAULT NULL,
    created_by_user_id BIGINT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipments_origin_wh FOREIGN KEY (origin_warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_shipments_dest_address FOREIGN KEY (destination_address_id) REFERENCES addresses(id),
    CONSTRAINT fk_shipments_carrier FOREIGN KEY (carrier_id) REFERENCES carriers(id),
    CONSTRAINT fk_shipments_status FOREIGN KEY (status_id) REFERENCES shipment_statuses(id),
    CONSTRAINT fk_shipments_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_shipments_user FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3.2 Shipment Items Junction Table (FK -> shipments, inventory_items)
CREATE TABLE shipment_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_shipment_items_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_items_item FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT uk_shipment_item UNIQUE (shipment_id, inventory_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3.3 Customs Declarations Table (FK -> shipments, customs_statuses, hs_tariff_codes, users)
CREATE TABLE customs_declarations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    declaration_number VARCHAR(50) NOT NULL UNIQUE,
    shipment_id BIGINT NOT NULL,
    customs_status_id BIGINT NOT NULL,
    declaration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duty_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    hs_tariff_code_id BIGINT NOT NULL,
    inspected_by_user_id BIGINT DEFAULT NULL,
    compliance_notes TEXT,
    CONSTRAINT fk_customs_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id) ON DELETE CASCADE,
    CONSTRAINT fk_customs_status FOREIGN KEY (customs_status_id) REFERENCES customs_statuses(id),
    CONSTRAINT fk_customs_tariff FOREIGN KEY (hs_tariff_code_id) REFERENCES hs_tariff_codes(id),
    CONSTRAINT fk_customs_inspector FOREIGN KEY (inspected_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- 4. EJB ADVANCED FEATURES & AUDITING TABLES (3NF / BCNF)
-- ------------------------------------------------------------------------------

-- 4.1 Vendor Performance Evaluations Table (FK -> vendors)
CREATE TABLE vendor_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivery_score DECIMAL(5,2) NOT NULL, -- 0.00 to 100.00
    quality_score DECIMAL(5,2) NOT NULL,  -- 0.00 to 100.00
    compliance_score DECIMAL(5,2) NOT NULL, -- 0.00 to 100.00
    overall_rating DECIMAL(3,2) NOT NULL, -- 1.00 to 5.00
    evaluation_period VARCHAR(30) NOT NULL, -- e.g., '2026-Q1', 'MONTHLY_AUTO'
    CONSTRAINT fk_evaluations_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4.2 Logistics Audit Trail Table (FK -> users, audit_actions)
-- (Strict 3NF: No redundant user_email column; joined via user_id FK)
CREATE TABLE logistics_audit_trail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL,
    action_id BIGINT NOT NULL,
    target_entity VARCHAR(50) NOT NULL, -- e.g., 'Shipment', 'Vendor', 'CustomsDeclaration'
    entity_id BIGINT DEFAULT NULL,
    execution_time_ms BIGINT NOT NULL DEFAULT 0,
    compliance_flag BOOLEAN NOT NULL DEFAULT TRUE,
    ip_address VARCHAR(45),
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_action FOREIGN KEY (action_id) REFERENCES audit_actions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4.3 Timer Schedules Table (FK -> timer_statuses)
CREATE TABLE timer_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timer_name VARCHAR(100) NOT NULL UNIQUE,
    schedule_expression VARCHAR(100) NOT NULL,
    last_run TIMESTAMP NULL DEFAULT NULL,
    next_run TIMESTAMP NULL DEFAULT NULL,
    status_id BIGINT NOT NULL,
    execution_count BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_timer_status FOREIGN KEY (status_id) REFERENCES timer_statuses(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4.4 System Exceptions Log Table (FK -> failure_scenarios, severity_levels)
CREATE TABLE system_exceptions_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exception_class VARCHAR(255) NOT NULL,
    message TEXT,
    failure_scenario_id BIGINT NOT NULL,
    severity_id BIGINT NOT NULL,
    stack_trace TEXT,
    logged_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exception_scenario FOREIGN KEY (failure_scenario_id) REFERENCES failure_scenarios(id),
    CONSTRAINT fk_exception_severity FOREIGN KEY (severity_id) REFERENCES severity_levels(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4.6 Global Freight Tariff Rates Master Table
CREATE TABLE tariff_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rate_key VARCHAR(50) NOT NULL UNIQUE,
    rate_value DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4.7 Support Tickets Master Table
CREATE TABLE support_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'General Inquiry',
    shipment_tracking VARCHAR(100) DEFAULT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_support_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4.8 Support Ticket Messages Table
CREATE TABLE support_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    sender_name VARCHAR(100) NOT NULL,
    sender_role VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_user FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------------------
-- 5. PERFORMANCE INDEXES
-- ------------------------------------------------------------------------------
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_cities_country ON cities(country_id);
CREATE INDEX idx_addresses_city ON addresses(city_id);
CREATE INDEX idx_vendors_country ON vendors(country_id);
CREATE INDEX idx_vendors_compliance ON vendors(compliance_status_id);
CREATE INDEX idx_warehouses_address ON warehouses(address_id);
CREATE INDEX idx_items_sku ON inventory_items(sku);
CREATE INDEX idx_items_warehouse ON inventory_items(warehouse_id);
CREATE INDEX idx_items_vendor ON inventory_items(vendor_id);
CREATE INDEX idx_shipments_tracking ON shipments(tracking_number);
CREATE INDEX idx_shipments_status ON shipments(status_id);
CREATE INDEX idx_shipments_carrier ON shipments(carrier_id);
CREATE INDEX idx_customs_declaration ON customs_declarations(declaration_number);
CREATE INDEX idx_customs_status ON customs_declarations(customs_status_id);
CREATE INDEX idx_customs_tariff ON customs_declarations(hs_tariff_code_id);
CREATE INDEX idx_support_tickets_user ON support_tickets(user_id);
CREATE INDEX idx_support_messages_ticket ON support_messages(ticket_id);
CREATE INDEX idx_audit_timestamp ON logistics_audit_trail(timestamp);
CREATE INDEX idx_audit_action ON logistics_audit_trail(action_id);
