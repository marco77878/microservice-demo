# Project Requirements Specification

[English](REQUIREMENTS.md) | [简体中文](REQUIREMENTS_ZH.md)

This document details the complete requirements and acceptance criteria for the Spring Cloud Microservices Platform.

---

## 1. Background

Spring Cloud is an open-source microservices suite provided by the Pivotal team, comprising service discovery and registration, centralized configuration, distributed tracing, API gateways, load balancing, and circuit breakers.

---

## 2. Functional & Architecture Requirements

### 2.1 Multi-Service Orchestration via Single `docker-compose.yaml`
The system must orchestrate and start all required services using a single `docker-compose.yaml` file:
1. **UAA (User Account and Authentication)**:
   - Database username and password authentication.
   - GitHub OAuth2 authentication.
   - LDAP directory authentication.
2. **MySQL**: Relational database storing users and product data.
3. **LDAP Service**: Directory service for enterprise user management.
4. **Gateway**:
   - **Exclusively exposes external port `7573`**.
   - Protocol must be **HTTP** (no HTTPS).
5. **Configure**: Centralized configuration management service.
6. **Discovery**: Service registry and discovery service.
7. **Product**: Business microservice managing product records (auto-increment primary key + product name).

*(Note: Component consolidation is permitted if an architecture naturally assumes multiple roles, provided the core responsibilities and the single 7573 port constraint are preserved).*

---

### 2.2 Role Hierarchy & API Authorization Rules

#### Role Hierarchy:
- `PRODUCT_ADMIN` possesses all permissions of both `EDITOR` and `USER`.
- `EDITOR` possesses all permissions of `USER` plus modification capabilities.
- `USER` possesses read-only access.

#### API Permission Rules:
- **View Products API (`GET /products`)**: Requires role `USER` or higher (`USER`, `EDITOR`, `PRODUCT_ADMIN`).
- **Add Product API (`POST /products`)**: Requires role `EDITOR` or higher (`EDITOR`, `PRODUCT_ADMIN`).
- **Update Product API (`PUT /products/{id}`)**: Requires role `EDITOR` or higher (`EDITOR`, `PRODUCT_ADMIN`).
- **Delete Product API (`DELETE /products/{id}`)**: Requires role `EDITOR` or higher (`EDITOR`, `PRODUCT_ADMIN`).

---

### 2.3 Technology Stack
- **Framework**: Spring Boot (latest stable LTS, e.g. 3.1.x) + Spring Cloud (2022.x/2023.x).
- **Build Tool**: Apache Maven (multi-module project).
- **Language**: Java (JDK version must be explicitly declared in README, e.g. OpenJDK 17).

---

## 3. Deliverables

### 3.1 Web Login Interface
- Provide a web URL accessible via the Gateway port `7573` (e.g. `http://localhost:7573/login`).
- The page must allow users to switch or click between:
  1. Username and Password login (Database)
  2. GitHub OAuth2 login
  3. LDAP directory login

---

### 3.2 Predefined Accounts

#### Database Users (MySQL):
- Normal User: `username: user_1`, `password: user_1` -> Role: `ROLE_USER`
- Editor User: `username: editor_1`, `password: editor_1` -> Role: `ROLE_EDITOR`
- Product Admin: `username: adm_1`, `password: adm_1` -> Role: `ROLE_PRODUCT_ADMIN`

#### GitHub OAuth2:
- Upon successful GitHub authentication, automatically assign role **`ROLE_EDITOR`**.

#### LDAP Users:
- Normal User: `username: ldap_user_1`, `password: ldap_user_1` -> Role: `ROLE_USER`
- Editor User: `username: ldap_editor_1`, `password: ldap_editor_1` -> Role: `ROLE_EDITOR`
- Product Admin: `username: ldap_adm_1`, `password: ldap_adm_1` -> Role: `ROLE_PRODUCT_ADMIN`

---

### 3.3 Command Line (Bash / CURL) Commands
Provide actual executable CURL commands along with their verified outputs in the documentation:
1. Obtain Access Token via CURL (Password Grant format):
   ```bash
   curl -X POST -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=password&username=YOUR_USERNAME&password=YOUR_PASSWORD&client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET" \
     http://localhost:7573/uaa/token
   ```
2. Add product via CURL with Access Token authentication.
3. Update product via CURL with Access Token authentication.
4. Delete product via CURL with Access Token authentication.
5. Query product list via CURL with Access Token authentication.

---

## 4. Verification & Evaluation Criteria

The evaluation team will verify the deliverable directly on their servers by running:
```bash
# Pull or clone the repository
mvn clean install
docker-compose up
```
The evaluator will not modify more than 10 lines of configuration files.

### Evaluation Confirmation Checklist:
- [x] Declares required JDK version in README.
- [x] Builds all artifacts cleanly with Maven.
- [x] Automatically initializes MySQL and creates required databases and tables.
- [x] Automatically starts LDAP and provisions initial directory users.
- [x] All microservices start and register correctly.
- [x] All CURL commands produce expected outputs and properly enforce RBAC rules.
