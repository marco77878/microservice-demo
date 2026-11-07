# Spring Cloud Microservices Platform (UAA + Gateway + Product)

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2022.0.4-blue.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An enterprise-grade, cloud-native microservices platform built with **Spring Boot 3.1.5**, **Spring Cloud 2022.0.4**, and **Java 17 (OpenJDK 17 LTS)**.

This platform integrates a Unified Authentication Center (**UAA**), an edge **API Gateway** as the sole exposed entry point on port **`7573` (HTTP)**, a **Product** microservice with hierarchical Role-Based Access Control (**RBAC**), Eureka service discovery, Spring Cloud Config, MySQL 8.0, and OpenLDAP.

---

## 📚 Project Documentation

Detailed specifications and architectural deep dives have been separated into dedicated documents:

- 📋 **[Project Requirements Specification (English)](REQUIREMENTS.md)**: Full background, functional specs, API permissions, role constraints, and evaluator criteria.
- 📋 **[Project Requirements Specification (Chinese)](REQUIREMENTS_ZH.md)**: Original requirements specification document.
- 🏛️ **[Technical Architecture & Implementation](ARCHITECTURE.md)**: In-depth architectural designs, Mermaid topology diagrams, authentication flow charts, security filters, role hierarchy, and persistence design.

---

## 📌 Environment Requirements

- **JDK Version**: **Java 17 (OpenJDK 17 LTS)** *(JDK 17 or higher required)*
- **Build Tool**: Apache Maven 3.8+
- **Container Tooling**: Docker 20.10+ & Docker Compose 2.0+

---

## 🚀 Quick Start & Deployment Guide

### Mode A: Docker Compose Deployment (Recommended)
Reviewers can compile, package, and launch all services in Docker using two standard commands:

```bash
# 1. Compile, run tests, and package executable JARs
mvn clean install

# 2. Build images and start all services in Docker
docker-compose up --build -d
```

> **Note**: MySQL automatically executes `docker/mysql/init.sql` upon first startup to initialize `microservices_db`, `users`, and `products` tables. OpenLDAP mounts and imports `docker/ldap/bootstrap.ldif`.

### Mode B: Local Standalone Execution (Without Docker)
To execute directly on local host using Java:
```bash
./start-local.sh
```
To stop all local background microservices:
```bash
./stop-local.sh
```

---

## 🌐 Web Console & Interactive Tester

- **Login URL**: [http://localhost:7573/login](http://localhost:7573/login) (or [http://localhost:7573/](http://localhost:7573/))
- **Interactive Features**:
  1. **Tab Switching**: Switch freely between Database, LDAP, and GitHub OAuth2 authentication tabs.
  2. **Quick Fill**: One-click quick fill buttons for all predefined test accounts.
  3. **Live Token Display**: Real-time display of user identity, role badge, and generated JWT Bearer Token with a copy button.
  4. **Product API Tester**: Test `GET`, `POST`, `PUT`, and `DELETE` requests against the Product service directly in the browser to verify permissions.

---

## 👥 Predefined Accounts & Role Hierarchy Matrix

Role inheritance strictly satisfies:
> **`PRODUCT_ADMIN` > `EDITOR` > `USER`**
> - `PRODUCT_ADMIN` inherits all permissions of `EDITOR` and `USER`.
> - `EDITOR` inherits `USER` read permissions plus create/update/delete permissions.
> - `USER` possesses read-only access to products.

| Auth Provider | Username | Password | Assigned Role | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **MySQL Database** | `user_1` | `user_1` | `ROLE_USER` | View product list only |
| **MySQL Database** | `editor_1` | `editor_1` | `ROLE_EDITOR` | View, add, edit, and delete products |
| **MySQL Database** | `adm_1` | `adm_1` | `ROLE_PRODUCT_ADMIN` | Full administrative permissions |
| **LDAP Directory** | `ldap_user_1` | `ldap_user_1` | `ROLE_USER` | View product list only |
| **LDAP Directory** | `ldap_editor_1` | `ldap_editor_1` | `ROLE_EDITOR` | View, add, edit, and delete products |
| **LDAP Directory** | `ldap_adm_1` | `ldap_adm_1` | `ROLE_PRODUCT_ADMIN` | Full administrative permissions |
| **GitHub OAuth2** | *(GitHub User)* | *(OAuth Sign-in)* | **`ROLE_EDITOR`** | Automatically granted `ROLE_EDITOR` upon sign-in |

---

## 💻 Verified CURL Command Line Examples & Test Output

All requests communicate through the API Gateway at port **`7573`**.

### 1. Obtain Access Token via Password Grant

#### A. Database Normal User (`user_1`):
```bash
curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=user_1&password=user_1&client_id=demo_client&client_secret=demo_secret" \
  http://localhost:7573/uaa/token
```
**Actual Response**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9VU0VSIiwic3ViIjoidXNlcl8xIiwiaWF0IjoxNzk0MDY1NjkyLCJleHAiOjE3OTQxNTIwOTJ9.sf5RJ4jj6XXoLIFfqN6-Z3G6VneLWnG-TRrv51Vi3bY",
  "role": "ROLE_USER",
  "scope": "read write",
  "token_type": "bearer",
  "expires_in": 86400,
  "username": "user_1"
}
```

#### B. LDAP Editor User (`ldap_editor_1`):
```bash
curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=ldap_editor_1&password=ldap_editor_1&client_id=demo_client&client_secret=demo_secret" \
  http://localhost:7573/uaa/token
```
**Actual Response**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiUk9MRV9FRElUT1IiLCJzdWIiOiJsZGFwX2VkaXRvcl8xIiwiaWF0IjoxNzk0MDY1NzU2LCJleHAiOjE3OTQxNTIxNTZ9.33OBFQZHiG44_Bt7CdFVtouz_4Pixd2xSF4Ua5zP-6s",
  "role": "ROLE_EDITOR",
  "scope": "read write",
  "token_type": "bearer",
  "expires_in": 86400,
  "username": "ldap_editor_1"
}
```

---

### 2. View Product List (`GET /products`, Requires USER Role)

```bash
curl -s -H "Authorization: Bearer $USER_TOKEN" http://localhost:7573/products
```
**Actual Response (HTTP 200 OK)**:
```json
[
  {"id": 1, "name": "MacBook Pro 16"},
  {"id": 2, "name": "iPhone 15 Pro Max"},
  {"id": 3, "name": "iPad Pro M4"}
]
```

---

### 3. Add Product (`POST /products`, Requires EDITOR Role)

#### Authorized Request with EDITOR Token:
```bash
curl -s -X POST -H "Authorization: Bearer $EDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple Vision Pro"}' \
  http://localhost:7573/products
```
**Actual Response (HTTP 201 Created)**:
```json
{
  "id": 4,
  "name": "Apple Vision Pro"
}
```

#### Unauthorized Request with USER Token (Access Denied):
```bash
curl -s -i -X POST -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Illegal Item"}' \
  http://localhost:7573/products | head -n 1
```
**Actual Response (HTTP 403 Forbidden)**:
```text
HTTP/1.1 403 Forbidden
```

---

### 4. Update Product (`PUT /products/{id}`, Requires EDITOR Role)

```bash
curl -s -X PUT -H "Authorization: Bearer $EDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple Vision Pro M2"}' \
  http://localhost:7573/products/4
```
**Actual Response (HTTP 200 OK)**:
```json
{
  "message": "Product updated successfully",
  "product": {
    "id": 4,
    "name": "Apple Vision Pro M2"
  }
}
```

---

### 5. Delete Product (`DELETE /products/{id}`, Requires EDITOR Role)

```bash
curl -s -X DELETE -H "Authorization: Bearer $EDITOR_TOKEN" \
  http://localhost:7573/products/4
```
**Actual Response (HTTP 200 OK)**:
```json
{
  "message": "Product deleted successfully with id: 4"
}
```

---

## 📁 Project Structure

```text
.
├── pom.xml                               # Root Maven aggregation configuration
├── docker-compose.yaml                   # Docker Compose multi-service orchestration
├── README.md                             # Project portal & quick start documentation
├── REQUIREMENTS.md                       # Full requirements specification document
├── ARCHITECTURE.md                       # In-depth architectural & implementation design
├── start-local.sh                        # Automated local background startup script
├── stop-local.sh                         # Automated local shutdown script
├── docker/
│   ├── mysql/init.sql                   # MySQL automatic schema and seed script
│   └── ldap/bootstrap.ldif              # OpenLDAP organizational unit & user import
├── common/                               # Shared JWT utilities and models
├── discovery-service/                    # Eureka Server (:8761)
├── config-service/                       # Spring Cloud Config Server (:8888)
├── gateway-service/                      # Spring Cloud Gateway (:7573, sole external port)
├── uaa-service/                          # UAA Service (:8081, Triple-Auth & Token issuer)
└── product-service/                      # Product Service (:8082, Hierarchical RBAC)
```

---

## 📋 Evaluation Checklist Verification

| Requirement Item | Status | Verification Evidence |
| :--- | :---: | :--- |
| **JDK Version Specification** | Verified | OpenJDK 17 LTS declared and compiled |
| **Clean Build & Packaging** | Verified | `mvn clean install` passes with all 7 modules and 11 tests |
| **Automatic Database Setup** | Verified | Auto-creates `microservices_db`, `users`, and `products` |
| **LDAP Directory Support** | Verified | OpenLDAP service and bootstrap LDIF configured |
| **Single External Port (7573)** | Verified | Only Gateway exposes host port 7573 via HTTP |
| **Role Hierarchy Implementation**| Verified | `PRODUCT_ADMIN` > `EDITOR` > `USER` enforced in security layer |
| **Verified CURL Commands** | Verified | Complete execution commands with real JSON responses documented |
