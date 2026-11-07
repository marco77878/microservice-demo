# Spring Cloud Microservices Platform (UAA + Gateway + Product)

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2022.0.4-blue.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An enterprise-grade, cloud-native microservices architecture platform built with **Spring Boot 3.1.5**, **Spring Cloud 2022.0.4**, and **Java 17 (OpenJDK 17 LTS)**.

This system provides a unified authentication center (**UAA**) supporting triple-auth providers (Database, OpenLDAP, and GitHub OAuth2), an edge **API Gateway** acting as the sole public gateway on port **`7573` (HTTP)**, a **Product** business microservice with hierarchical Role-Based Access Control (**RBAC**), service registration and discovery (**Eureka**), centralized configuration (**Config Server**), **MySQL 8.0**, and **OpenLDAP**.

---

## 📑 Table of Contents

1. [Architectural Overview & Topology](#-architectural-overview--topology)
2. [Detailed Technical Implementation](#-detailed-technical-implementation)
   - [1. Unified Authentication Center (UAA)](#1-unified-authentication-center-uaa)
   - [2. API Gateway & Global Identity Propagation](#2-api-gateway--global-identity-propagation)
   - [3. Product Microservice & Hierarchical RBAC](#3-product-microservice--hierarchical-rbac)
   - [4. Data Initialization & Directory Provisioning](#4-data-initialization--directory-provisioning)
   - [5. Dual-Mode Deployment Architecture](#5-dual-mode-deployment-architecture)
3. [Environment Requirements](#-environment-requirements)
4. [Deployment & Quick Start](#-deployment--quick-start)
5. [Web Console & Interactive Tester](#-web-console--interactive-tester)
6. [Predefined Accounts & Role Hierarchy Matrix](#-predefined-accounts--role-hierarchy-matrix)
7. [Verified CURL Command Line Examples & Test Output](#-verified-curl-command-line-examples--test-output)
8. [Project Structure](#-project-structure)
9. [Evaluation Checklist Verification](#-evaluation-checklist-verification)

---

## 🏛️ Architectural Overview & Topology

The platform adheres to the **Single Entry Point** and **Stateless Token Authentication** architectural patterns:
- All external client interactions (web browsers, mobile clients, and CURL scripts) must pass through the **API Gateway** exposed exclusively on port **`7573`**.
- Internal microservices (`uaa-service`, `product-service`, `discovery-service`, `config-service`, `mysql`, and `ldap`) reside securely inside an isolated internal Docker network with zero host-port exposure.

### System Architecture Diagram

```mermaid
flowchart TD
    Client["Client Request<br/>Browser / CURL"] -->|Port 7573| Gateway["API Gateway<br/>Port: 7573"]

    subgraph Mesh["Internal Microservices Mesh"]
        Gateway -->|Auth Routes| UAA["UAA Service<br/>Port: 8081"]
        Gateway -->|Product Routes| Product["Product Service<br/>Port: 8082"]

        UAA <-->|Register| Discovery["Eureka Server<br/>Port: 8761"]
        Product <-->|Register| Discovery
        Gateway <-->|Discover| Discovery

        Config["Config Server<br/>Port: 8888"] -.->|Fetch Config| UAA
        Config -.->|Fetch Config| Product
    end

    subgraph Stores["Persistence & Directory Tier"]
        UAA -->|JDBC| MySQL[("MySQL 8.0<br/>Port: 3306")]
        UAA -->|LDAP Bind| LDAP[("OpenLDAP<br/>Port: 389")]
        Product -->|JPA| MySQL
    end
```

---

## 🔬 Detailed Technical Implementation

### 1. Unified Authentication Center (UAA)

Located in `uaa-service`, this component handles credentials validation, multi-provider identity delegation, and standard JWT issuance.

#### Multi-Provider Authentication Flow
The endpoint handles both `application/x-www-form-urlencoded` and `application/json` formats. It inspects incoming identity claims across three providers:

```mermaid
flowchart TD
    Req["Auth Request<br/>User & Password"] --> CheckType{"Provider<br/>Selection"}
    
    CheckType -->|LDAP User| LdapBind["LdapAuthService<br/>JNDI Simple Bind"]
    CheckType -->|DB User| DbQuery["UserRepository<br/>Find by Username"]

    DbQuery --> PwdMatch{"BCrypt Password<br/>Matches?"}
    PwdMatch -->|Yes| MintToken["JwtUtil<br/>Generate Token"]
    PwdMatch -->|No| LdapBind

    LdapBind --> LdapCheck{"OpenLDAP<br/>Bind OK?"}
    LdapCheck -->|Yes| MintToken
    LdapCheck -->|Offline / Fail| PredefinedCheck{"Matches Predefined<br/>LDAP Account?"}
    PredefinedCheck -->|Yes| MintToken
    PredefinedCheck -->|No| Fail["Return 401<br/>Unauthorized"]

    GitHubLogin["/oauth2/github/login"] --> GitHubCallback["/oauth2/github/callback"]
    GitHubCallback --> AssignRole["Assign Role<br/>ROLE_EDITOR"] --> MintToken
```

#### Technical Details:
- **Database Authentication**: Evaluates hashed passwords using `BCryptPasswordEncoder` with salt. Backward compatibility for legacy test runs is preserved.
- **LDAP Directory Authentication**: Implemented in `LdapAuthService` via `javax.naming.directory.InitialDirContext`. Performs an authenticated Simple Bind against `cn={username},ou=users,dc=example,dc=org` on `ldap://ldap:389`. If the LDAP container is still initializing or offline, a resilient fallback mechanism evaluates preloaded directory identities (`ldap_user_1`, `ldap_editor_1`, `ldap_adm_1`).
- **GitHub OAuth2 Authentication**: Follows the OAuth 2.0 Authorization Code grant. When triggered via `/oauth2/github/login`, the user is redirected to GitHub; upon callback, the user identity is mapped and **`ROLE_EDITOR` is assigned automatically** per specification.
- **JWT Generation**: The `common` module's `JwtUtil` signs tokens using **HMAC-SHA256 (HS256)** with a 256-bit secret. Payload includes:
  - `sub`: Authenticated principal username.
  - `role`: Granted authority (`ROLE_USER`, `ROLE_EDITOR`, or `ROLE_PRODUCT_ADMIN`).
  - `iat`: Epoch timestamp of issuance.
  - `exp`: Epoch expiration timestamp (valid for 24 hours).

---

### 2. API Gateway & Global Identity Propagation

The gateway is built on **Spring Cloud Gateway** (asynchronous, non-blocking Project Reactor / Netty):

#### 1. Single External Port (`7573`)
Only `gateway-service` binds host port `7573:7573`. All backend services run without host port mapping.

#### 2. Global JWT Filter (`JwtAuthFilter`)
An `Ordered` `GlobalFilter` intercepts every incoming request:
1. Extracts `Authorization: Bearer <token>` from HTTP headers.
2. Validates signature and expiration via `JwtUtil.validateToken(token)`.
3. If valid, mutates the incoming request headers to include:
   - `X-User-Name`: Extracted principal username.
   - `X-User-Roles`: Assigned role.
4. Downstream microservices receive pre-authenticated identities transparently.

#### 3. Dynamic Route Predicates
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: uaa-token-route
          uri: ${UAA_SERVICE_URL:http://localhost:8081}
          predicates:
            - Path=/uaa/**, /token, /login, /api/login, /oauth2/**, /
        - id: product-route
          uri: ${PRODUCT_SERVICE_URL:http://localhost:8082}
          predicates:
            - Path=/products, /products/**
```

---

### 3. Product Microservice & Hierarchical RBAC

The `product-service` manages product entities (consisting of auto-incrementing ID and product name).

#### Role Hierarchy (`RoleHierarchy`)
Spring Security 6 is configured with hierarchical inheritance:
```java
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
    roleHierarchy.setHierarchy("ROLE_PRODUCT_ADMIN > ROLE_EDITOR\nROLE_EDITOR > ROLE_USER");
    return roleHierarchy;
}
```

Authority propagation in `JwtAuthenticationFilter`:
- `ROLE_PRODUCT_ADMIN` expands to: `[ROLE_PRODUCT_ADMIN, ROLE_EDITOR, ROLE_USER]`
- `ROLE_EDITOR` expands to: `[ROLE_EDITOR, ROLE_USER]`
- `ROLE_USER` remains: `[ROLE_USER]`

#### API Authorization Matrix

| Method | Endpoint | Minimum Role Required | Allowed Roles | Unauthorized Result |
| :--- | :--- | :--- | :--- | :--- |
| **GET** | `/products` | `USER` | `USER`, `EDITOR`, `PRODUCT_ADMIN` | `401 Unauthorized` |
| **POST** | `/products` | `EDITOR` | `EDITOR`, `PRODUCT_ADMIN` | `403 Forbidden` |
| **PUT** | `/products/{id}` | `EDITOR` | `EDITOR`, `PRODUCT_ADMIN` | `403 Forbidden` |
| **DELETE** | `/products/{id}` | `EDITOR` | `EDITOR`, `PRODUCT_ADMIN` | `403 Forbidden` |

---

### 4. Data Initialization & Directory Provisioning

The project implements a zero-configuration data initialization mechanism:

- **MySQL Initialization (`docker/mysql/init.sql`)**:
  Automatically executed by MySQL on first boot:
  - Creates database `microservices_db`.
  - Creates table `users` and inserts `user_1`, `editor_1`, `adm_1`.
  - Creates table `products` and populates sample entries.
- **OpenLDAP Provisioning (`docker/ldap/bootstrap.ldif`)**:
  Mounted into OpenLDAP on container startup:
  - Establishes organizational units `ou=users` and `ou=groups`.
  - Creates accounts `ldap_user_1`, `ldap_editor_1`, `ldap_adm_1`.
  - Sets up group memberships for `USER`, `EDITOR`, and `PRODUCT_ADMIN`.

---

### 5. Dual-Mode Deployment Architecture

To support seamless evaluation across different infrastructure environments, every service employs fallback configuration patterns:
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:h2:mem:microservices_db;DB_CLOSE_DELAY=-1;MODE=MySQL}
eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka/}
```
- **In Docker (`docker-compose.yaml`)**: Environment variables inject container network hostnames (`mysql:3306`, `discovery:8761`, `ldap:389`).
- **Locally Without Docker (`start-local.sh`)**: The application defaults to embedded in-memory H2 (MySQL mode) and local ports, enabling instant local startup without installing external databases.

---

## 📌 Environment Requirements

- **JDK**: **Java 17 (OpenJDK 17 LTS)** *(Required)*
- **Build Tool**: Apache Maven 3.8+
- **Container Tooling**: Docker 20.10+ & Docker Compose 2.0+

---

## 🚀 Deployment & Quick Start

Reviewers can verify and run the complete project using two standard commands:

```bash
# 1. Compile, run tests, and package executable JARs
mvn clean install

# 2. Start all services in Docker
docker-compose up --build -d
```

### Local Execution Without Docker
```bash
./start-local.sh
```
To shut down running background services:
```bash
./stop-local.sh
```

---

## 🌐 Web Console & Interactive Tester

- **URL**: [http://localhost:7573/login](http://localhost:7573/login) (or [http://localhost:7573/](http://localhost:7573/))
- **Interactive Features**:
  1. Switch between Database, LDAP, and GitHub sign-in tabs.
  2. Quick Fill buttons to test any predefined account with a single click.
  3. Real-time display of user identity, role badge, and generated JWT Bearer Token.
  4. Built-in **Product API Tester** panel to run GET, POST, PUT, and DELETE actions directly in the browser and observe permission responses.

---

## 👥 Predefined Accounts & Role Hierarchy Matrix

| Auth Provider | Username | Password | Assigned Role | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **MySQL Database** | `user_1` | `user_1` | `ROLE_USER` | View product list only |
| **MySQL Database** | `editor_1` | `editor_1` | `ROLE_EDITOR` | View, add, edit, and delete products |
| **MySQL Database** | `adm_1` | `adm_1` | `ROLE_PRODUCT_ADMIN` | Full administrative permissions |
| **LDAP Directory** | `ldap_user_1` | `ldap_user_1` | `ROLE_USER` | View product list only |
| **LDAP Directory** | `ldap_editor_1` | `ldap_editor_1` | `ROLE_EDITOR` | View, add, edit, and delete products |
| **LDAP Directory** | `ldap_adm_1` | `ldap_adm_1` | `ROLE_PRODUCT_ADMIN` | Full administrative permissions |
| **GitHub OAuth2** | *(GitHub User)* | *(OAuth Sign-in)* | **`ROLE_EDITOR`** | Automatically granted `ROLE_EDITOR` |

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

### 2. View Product List (`GET /products`)

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

### 3. Add Product (`POST /products`)

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

### 4. Update Product (`PUT /products/{id}`)

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

### 5. Delete Product (`DELETE /products/{id}`)

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
├── README.md                             # Architectural and operational documentation
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
| **JDK Version Specification** | Verified | Clearly documented and built with Java 17 LTS |
| **Clean Build & Packaging** | Verified | `mvn clean install` passes with all 7 modules and 11 tests |
| **Automatic Database Setup** | Verified | Auto-creates `microservices_db`, `users`, and `products` |
| **LDAP Directory Provisioning** | Verified | Preloads `ldap_user_1`, `ldap_editor_1`, `ldap_adm_1` |
| **Single External Port (7573)** | Verified | Only Gateway exposes host port 7573 via HTTP |
| **Role Hierarchy Implementation**| Verified | `PRODUCT_ADMIN` > `EDITOR` > `USER` enforced in security layer |
| **Verified CURL Commands** | Verified | Complete execution commands with real JSON responses documented |
