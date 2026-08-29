# GlobalTrade Logistics Corporation 🌍🚢✈️

[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10.0-blue.svg?style=for-the-badge&logo=jakartaee)](https://jakarta.ee/)
[![WildFly](https://img.shields.io/badge/WildFly-30.0%2B-red.svg?style=for-the-badge&logo=redhat)](https://www.wildfly.org/)
[![ActiveMQ](https://img.shields.io/badge/Apache%20ActiveMQ-6.2-blueviolet.svg?style=for-the-badge&logo=apache)](https://activemq.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-00758F.svg?style=for-the-badge&logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

> **Enterprise Java EE Multi-Module Application for Global Supply Chain, Tariff Compliance & Real-Time Logistics Management**

---

## 📌 Executive Overview

**GlobalTrade Logistics Corporation** is a full-featured, enterprise-grade Java EE application designed to manage complex global supply chain workflows. Built on a multi-tier **Jakarta EE 10 / EJB 3.x** architecture and backed by a strictly normalized **3NF / BCNF MySQL database**, the system orchestrates international shipment dispatches, customs tariff declarations, vendor performance tracking, and real-time cargo telemetry.

Key enterprise highlights include:
- **Asynchronous Processing**: Integrated **Apache ActiveMQ JMS** brokers for instant event dispatch and decoupled background processing.
- **Automated Timer Services**: EJB `@Schedule` timers for compliance monitoring, SLA enforcement, and automated status synchronization.
- **Enterprise Security**: JAAS Role-Based Access Control (RBAC) securing Admin, Customer, Carrier, and Customs Officer portals.
- **High-Performance Telemetry**: Built-in operational dashboards and live JVM/Server performance metrics visualization.

---

## 🖼️ System Interfaces Showcase

Here is a visual overview of the GlobalTrade management suites and web portals:

### 1. 📊 Executive Admin Dashboard & Performance Metrics
Operational intelligence monitoring live shipment stats, revenue telemetry, active EJB pools, and server metrics.

| **Admin Control Center** | **System Performance Metrics** |
| :---: | :---: |
| ![Admin Dashboard](UserInterfaces/Admin%20Dashboard.png) | ![Admin System Performance Metrics](UserInterfaces/Admin%20System%20Performance%20Metrics.png) |

---

### 2. 🌐 Customer Portal & Global Freight Booking
Self-service customer interface for booking international freight, calculating tariff estimates, and downloading invoices.

| **Customer Portal & Home** | **Shipment Booking & Rates** |
| :---: | :---: |
| ![Customer Home](UserInterfaces/Customer%20Home.png) | ![Book Shipment](UserInterfaces/Book%20Shipment.png) |

---

### 3. 📍 Shipment Tracking & Customs Clearance Matrix
End-to-end cargo visibility, customs tariff classification, and Harmonized System (HS) code duty declarations.

| **Real-Time Cargo Tracking** | **Customs Duty Declaration** |
| :---: | :---: |
| ![Shipment Tracking](UserInterfaces/Shipment%20Tracking.png) | ![Admin Custom Declaration](UserInterfaces/Admin%20Custom%20Declaration.png) |

---

## 🏗️ Architecture & Module Structure

The project follows a standard multi-module Maven layout packaging business components into an Enterprise Archive (`.ear`).

```mermaid
graph TD
    subgraph Client Layer
        WebUser[🌐 Customer Portal / Web Browser]
        AdminUser[👨‍💼 Admin Console]
        MQClient[📡 Standalone JMS MQ Client]
    end

    subgraph Presentation & API Layer (globaltrade-web)
        Servlet[Jakarta Servlet 6.0 / JSP]
        REST[RESTful Web Services]
    end

    subgraph Enterprise Business Layer (globaltrade-ejb)
        SLSB[Stateless Session Beans - Shipment & Tariff Services]
        SFSB[Stateful Session Beans - Booking & Cart Sessions]
        MDB[Message-Driven Beans - ActiveMQ JMS Listeners]
        Timers[EJB Background Timers - SLA & Audit Processing]
    end

    subgraph Infrastructure & Storage
        JMSBroker[📨 Apache ActiveMQ JMS Broker]
        JPA[Persistence Layer / Hibernate ORM]
        DB[(MySQL 8.0 Database 3NF/BCNF)]
    end

    WebUser --> Servlet
    AdminUser --> Servlet
    Servlet --> SLSB
    Servlet --> SFSB
    MQClient --> JMSBroker
    JMSBroker --> MDB
    MDB --> SLSB
    SLSB --> JPA
    SFSB --> JPA
    Timers --> JPA
    JPA --> DB
```

### Module Breakdown

| Module | Type | Description |
| :--- | :--- | :--- |
| 📦 [`globaltrade-ejb`](global-trade-logistic-corporation-core/globaltrade-ejb) | EJB Module | Core business logic, EJB 3.x Session Beans (SLSB/SFSB), Message-Driven Beans (MDBs), EJB Timer Services, and JPA ORM Entities. |
| 🌐 [`globaltrade-web`](global-trade-logistic-corporation-core/globaltrade-web) | Dynamic Web | Web presentation layer, HTML5/JS Customer & Admin Portals, Servlets, and RESTful web services. |
| 📦 [`globaltrade-mq-client`](global-trade-logistic-corporation-core/globaltrade-mq-client) | Java Application | Standalone ActiveMQ JMS Producer/Consumer client for automated messaging and external integration. |
| 📦 [`globaltrade-ear`](global-trade-logistic-corporation-core/globaltrade-ear) | Enterprise EAR | Bundles `globaltrade-ejb` and `globaltrade-web` into a single deployable EAR archive. |
| 🧪 [`globaltrade-testing`](global-trade-logistic-corporation-core/globaltrade-testing) | Integration | JUnit 5 unit and integration test suite for EJB business component verification. |
| 🗄️ [`Database`](Database) | SQL Scripts | Strictly normalized MySQL 8.0 schema (`schema.sql`) complying with 3NF / BCNF rules. |

---

## 🔥 Key Features

- **🚢 End-to-End Shipment Management**: Lifecycle state machine handling shipment draft, booking, carrier assignment, dispatch, customs hold, in-transit, and delivery verification.
- **🛃 International Customs & HS Tariff Engine**: Calculation of customs duties using Harmonized System (HS) tariff code matrices and automated duty computation.
- **🏬 Vendor & Inventory Compliance Scoring**: Real-time evaluation of supplier fulfillment rates, compliance audits, and stock level tracking.
- **⚡ ActiveMQ Messaging Integration**: JMS queues for high-volume asynchronous transaction logs, cargo status dispatches, and emergency alerts.
- **⏱️ Automated EJB Timer Services**: Cron-style background scheduled jobs executing tariff matrix updates, overdue shipment escalation, and telemetry metric rollups.
- **🔒 Role-Based Access Control (RBAC)**: Fine-grained security permissions separating system administrators, logistics coordinators, customs agents, and retail customers.

---

## 🗄️ Database Architecture (3NF / BCNF)

The underlying database schema (`Database/schema.sql`) is structured into decoupled reference tables and transactional entities to eliminate functional dependency anomalies:

- **Reference Masters**: `countries`, `cities`, `addresses`, `carriers`, `hs_tariff_codes`, `vendor_compliance_statuses`, `shipment_statuses`, `customs_statuses`.
- **Core Entities**: `users`, `roles`, `customers`, `vendors`, `products`, `inventories`, `shipments`, `shipment_items`, `customs_declarations`, `invoices`, `help_tickets`.

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed on your environment:
- **Java Development Kit (JDK)**: 17 or higher
- **Build Tool**: Apache Maven 3.8+
- **Application Server**: WildFly 30+ / Payara 6+ / GlassFish 7+
- **Database**: MySQL Server 8.0+ / MariaDB 10.5+
- **Message Broker**: Apache ActiveMQ 6.x

---

### Step 1: Database Initialization

Create and populate the database using the provided SQL script:

```bash
# Log into MySQL CLI
mysql -u root -p < Database/schema.sql
```

---

### Step 2: Configure Application Server Data Source

Set up a JTA Data Source in your application server (e.g., WildFly `standalone.xml`):

```xml
<datasource jndi-name="java:jboss/datasources/GlobalTradeDS" pool-name="GlobalTradeDS" enabled="true">
    <connection-url>jdbc:mysql://localhost:3306/globaltrade_db?useSSL=false&amp;serverTimezone=UTC</connection-url>
    <driver>mysql</driver>
    <security>
        <user-name>root</user-name>
        <password>YOUR_PASSWORD</password>
    </security>
</datasource>
```

---

### Step 3: Build the Multi-Module Project

Execute the Maven build command from the core directory:

```bash
cd global-trade-logistic-corporation-core
mvn clean install
```

This compiles all modules, runs JUnit tests, and packages the EAR archive at:
`globaltrade-ear/target/globaltrade-ear-1.0-SNAPSHOT.ear`

---

### Step 4: Deploy & Access

1. Deploy `globaltrade-ear-1.0-SNAPSHOT.ear` to your application server's deployment directory (or via Admin Console).
2. Start Apache ActiveMQ broker for JMS messaging support.
3. Access the web portal in your browser:
   - **Customer Portal**: `http://localhost:8080/globaltrade/customer/`
   - **Admin Control Center**: `http://localhost:8080/globaltrade/index.html`

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for details.

---

<p align="center">
  Developed for <b>GlobalTrade Logistics Corporation</b> • Built with Jakarta EE & Enterprise Java Beans
</p>
