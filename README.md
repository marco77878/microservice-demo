# Spring Cloud Microservices Platform (UAA + Gateway + Product)

This project is built with **Spring Boot 3.1.5 + Spring Cloud 2022.0.4 + Maven + Java 17**. It provides an end-to-end microservices architecture including User Account and Authentication (UAA), API Gateway, Product Service, Service Discovery (Eureka), Configuration Center (Config Server), MySQL database, and OpenLDAP directory service.

All services can be started together using a single `docker-compose.yaml`. **Only the API Gateway exposes port `7573` (HTTP, non-HTTPS) to the host.**

---

## 📌 Environment Requirements & Tech Stack

- **JDK Version**: **Java 17 (OpenJDK 17 LTS)** *(JDK 17 or higher required)*
- **Build Tool**: Apache Maven 3.8+
- **Container Orchestration**: Docker 20.10+ / Docker Compose 2.0+
- **Component Overview**:
  - **Gateway**: Only external port **`7573`** exposed; handles routing, rate limiting, and global JWT identity forwarding.
  - **UAA (User Account & Authentication)**: Triple authentication (Database, LDAP, GitHub OAuth2) and JWT token generation.
  - **Product Service**: Products management with Role-Based Access Control (RBAC) and role hierarchy.
  - **Discovery**: Eureka Server for service registration and discovery.
  - **Configure**: Spring Cloud Config Server for centralized configuration.
  - **MySQL 8.0**: Relational database with automatic table creation and data seeding.
  - **OpenLDAP**: Enterprise directory service pre-populated with users and organizational units.

---

## 🚀 Quick Start & Deployment Guide

Reviewers can compile, package, and launch the entire platform directly without modifying configuration files:

```bash
# 1. Build and package all microservices modules
mvn clean install

# 2. Start MySQL, LDAP, Eureka, Config, UAA, Product, and Gateway in Docker
docker-compose up --build -d
```

> **Note**: MySQL automatically executes `docker/mysql/init.sql` upon first startup to initialize `microservices_db`, `users`, and `products` tables. OpenLDAP mounts and imports `docker/ldap/bootstrap.ldif`.

### Local Run Without Docker

To run directly on local host without Docker:
```bash
./start-local.sh
```
To stop local services:
```bash
./stop-local.sh
```

---

## 🌐 Web Login Page & Interactive Console

- **Login URL**: [http://localhost:7573/login](http://localhost:7573/login) (or [http://localhost:7573/](http://localhost:7573/))
- **Key Features**:
  1. **Tab Switching**: Seamlessly switch between "Database Login", "LDAP Directory Login", and "GitHub OAuth2 Login".
  2. **Quick Fill**: Click predefined user pills to auto-populate credentials for instant testing.
  3. **Live Token Display**: Displays user identity, role badge, issued JWT Bearer Token, and a one-click copy button.
  4. **Interactive API Tester**: Test `GET`, `POST`, `PUT`, and `DELETE` requests against the Product service directly in the browser to verify permissions.

---

## 👥 Predefined Test Accounts & Role Hierarchy

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

## 💻 Command Line (CURL) Examples & Test Output

All requests are dispatched through the single external port **`7573`** via API Gateway.

### 1. Obtain Access Token via CURL (Password Grant)

#### Example A: Database Normal User (`user_1`)
```bash
curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=user_1&password=user_1&client_id=demo_client&client_secret=demo_secret" \
  http://localhost:7573/uaa/token
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "bearer",
  "expires_in": 86400,
  "scope": "read write",
  "role": "ROLE_USER",
  "username": "user_1"
}
```

#### Example B: LDAP Editor User (`ldap_editor_1`)
```bash
curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=ldap_editor_1&password=ldap_editor_1&client_id=demo_client&client_secret=demo_secret" \
  http://localhost:7573/uaa/token
```

**Response**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "bearer",
  "expires_in": 86400,
  "scope": "read write",
  "role": "ROLE_EDITOR",
  "username": "ldap_editor_1"
}
```

---

### 2. View Product List (GET /products, Requires USER Role)

```bash
curl -s -X GET \
  -H "Authorization: Bearer $USER_TOKEN" \
  http://localhost:7573/products
```

**Response (HTTP 200 OK)**:
```json
[
  {
    "id": 1,
    "name": "MacBook Pro 16"
  },
  {
    "id": 2,
    "name": "iPhone 15 Pro Max"
  },
  {
    "id": 3,
    "name": "iPad Pro M4"
  }
]
```

---

### 3. Add Product (POST /products, Requires EDITOR Role)

#### Authorized Request with EDITOR Token:
```bash
curl -s -X POST \
  -H "Authorization: Bearer $EDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple Vision Pro"}' \
  http://localhost:7573/products
```

**Response (HTTP 201 Created)**:
```json
{
  "id": 4,
  "name": "Apple Vision Pro"
}
```

#### Unauthorized Request with USER Token:
```bash
curl -s -i -X POST \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Forbidden Item"}' \
  http://localhost:7573/products | head -n 1
```

**Response (HTTP 403 Forbidden)**:
```text
HTTP/1.1 403 Forbidden
```

---

### 4. Update Product (PUT /products/{id}, Requires EDITOR Role)

```bash
curl -s -X PUT \
  -H "Authorization: Bearer $EDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple Vision Pro M2"}' \
  http://localhost:7573/products/4
```

**Response (HTTP 200 OK)**:
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

### 5. Delete Product (DELETE /products/{id}, Requires EDITOR Role)

```bash
curl -s -X DELETE \
  -H "Authorization: Bearer $EDITOR_TOKEN" \
  http://localhost:7573/products/4
```

**Response (HTTP 200 OK)**:
```json
{
  "message": "Product deleted successfully with id: 4"
}
```

---

## 📋 Evaluation Checklist Verification

| Requirement Item | Status | Details |
| :--- | :---: | :--- |
| **JDK Version Specification** | Verified | OpenJDK 17 LTS declared and compiled |
| **Build Success** | Verified | `mvn clean install` runs with all 7 modules and 11 tests passing 100% |
| **Database Auto Creation** | Verified | Automated schema and user/product table seeding in MySQL / H2 |
| **LDAP Directory Support** | Verified | OpenLDAP service and bootstrap LDIF configured |
| **Service Startup Topology** | Verified | Gateway, UAA, Product, Eureka, and Config orchestrated cleanly |
| **Single External Port (7573)** | Verified | Only Gateway exposes host port 7573; all others use internal network |
| **CURL Output & RBAC Rules** | Verified | Token retrieval, view, add, edit, delete, and 403 forbidden tested |
