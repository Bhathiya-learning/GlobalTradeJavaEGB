# GlobalTrade Logistics Corporation - Core Enterprise System 🌍🚢✈️

[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Jakarta EE](https://img.shields.io/badge/Jakarta%20EE-10.0-blue.svg?style=for-the-badge&logo=jakartaee)](https://jakarta.ee/)
[![WildFly](https://img.shields.io/badge/WildFly-30.0%2B-red.svg?style=for-the-badge&logo=redhat)](https://www.wildfly.org/)
[![ActiveMQ](https://img.shields.io/badge/Apache%20ActiveMQ-6.2-blueviolet.svg?style=for-the-badge&logo=apache)](https://activemq.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-00758F.svg?style=for-the-badge&logo=mysql)](https://www.mysql.com/)

> **Enterprise Java EE Multi-Module Maven Core for Global Supply Chain & Real-Time Logistics Management**

---

## 🖼️ UI Screenshots Overview

| **Admin Dashboard** | **System Metrics** |
| :---: | :---: |
| ![Admin Dashboard](../UserInterfaces/Admin%20Dashboard.png) | ![Admin System Performance Metrics](../UserInterfaces/Admin%20System%20Performance%20Metrics.png) |

| **Customer Home** | **Shipment Tracking** |
| :---: | :---: |
| ![Customer Home](../UserInterfaces/Customer%20Home.png) | ![Shipment Tracking](../UserInterfaces/Shipment%20Tracking.png) |

---

## 📦 Project Modules

This folder contains the Maven multi-module structure:
* `globaltrade-ejb`: Stateful/Stateless Session Beans, MDBs, JPA Entities, EJB Timers.
* `globaltrade-web`: Servlet 6.0 Servlets, RESTful APIs, Admin & Customer Portals.
* `globaltrade-mq-client`: Standalone JMS client application for Apache ActiveMQ.
* `globaltrade-ear`: Enterprise Application Archive EAR bundler.
* `globaltrade-testing`: Integration test suite using JUnit 5.

---

## 🛠️ Quick Build Command

```bash
mvn clean install
```
Packaging output: `globaltrade-ear/target/globaltrade-ear-1.0-SNAPSHOT.ear`
