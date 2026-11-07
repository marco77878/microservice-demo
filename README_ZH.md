# Spring Cloud 微服务全栈平台 (UAA + Gateway + Product)

[English](README.md) | [简体中文](README_ZH.md)

[![Java 17](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2022.0.4-blue.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

基于 **Spring Boot 3.1.5 + Spring Cloud 2022.0.4 + Java 17 (OpenJDK 17 LTS)** 构建的企业级云原生微服务平台。

本系统集成了统一认证中心（**UAA**）、全系统唯一对外开放的 API 网关（**Gateway，端口 7573**）、支持角色继承 RBAC 鉴权的产品业务服务（**Product**）、Eureka 服务注册与发现中心、Spring Cloud Config 统一配置中心、MySQL 8.0 数据库以及 OpenLDAP 目录服务。

---

## 📚 项目全套文档索引

- 📋 **[需求规格说明书 (中文)](REQUIREMENTS_ZH.md)** | **[Requirements Specification (English)](REQUIREMENTS.md)**：包含业务背景、多服务拓扑要求、单端口约束、角色层级模型与评审验收准则。
- 🏛️ **[技术架构与底层实现 (中文)](ARCHITECTURE_ZH.md)** | **[Architecture & Implementation (English)](ARCHITECTURE.md)**：深度技术剖析、Mermaid 架构拓扑图、多源认证决策流程、响应式网关透明透传、RBAC 角色继承与双模部署设计。

---

## 📌 环境要求与技术栈

- **JDK 版本**：**Java 17 (OpenJDK 17 LTS)** *(强制要求 JDK 17 及以上)*
- **构建管理**：Apache Maven 3.8+
- **容器与编排**：Docker 20.10+ & Docker Compose 2.0+

---

## 🚀 极简部署与启动指南

### 模式 A：Docker Compose 容器化部署（评审组推荐）
在项目根目录下直接运行以下两条标准命令，即可完成全套服务的自动化编译打包与容器编排拉起：

```bash
# 1. 编译全量微服务并打包生成 target/*.jar
mvn clean install

# 2. 自动化构建镜像并拉起所有微服务与中间件
docker-compose up --build -d
```

> **说明**：MySQL 首次启动将自动执行 `docker/mysql/init.sql` 完成 `microservices_db` 建库、建表及账号初始化；OpenLDAP 容器自动挂载导入 `docker/ldap/bootstrap.ldif`。

### 模式 B：本地轻量级启动（无 Docker 依赖）
如果本地未安装 Docker，也可直接通过预置脚本秒级拉起：
```bash
./start-local.sh
```
如需停止后台运行的服务：
```bash
./stop-local.sh
```

---

## 🌐 登录控制台与前端交互测试

- **登录地址**：[http://localhost:7573/login](http://localhost:7573/login) （或访问 [http://localhost:7573/](http://localhost:7573/)）
- **核心功能**：
  1. **三合一无缝切换**：支持可视化切换「数据库密码登录」、「LDAP 目录登录」、「GitHub OAuth2 登录」。
  2. **一键快速填入**：内置所有预置账号的快速填充按钮，无需手动繁琐输入。
  3. **实时展示与复制**：登录成功后展示用户身份、对应角色 Badge、签发的 JWT Bearer Token 及一键复制按钮。
  4. **产品服务测试控制台**：页面下方附带针对 Product 接口的实时测试按钮，可直观验证增删改查权限与拦截状态。

---

## 👥 预置测试账号与角色继承矩阵

系统角色层级关系严格满足继承要求：
> **`PRODUCT_ADMIN` > `EDITOR` > `USER`**
> - `PRODUCT_ADMIN` 拥有 `EDITOR` 和 `USER` 的所有权限；
> - `EDITOR` 拥有 `USER` 的只读浏览权限以及增删改权限；
> - `USER` 仅拥有浏览产品权限。

| 认证源 | 用户名 | 密码 | 拥有角色 | 权限说明 |
| :--- | :--- | :--- | :--- | :--- |
| **MySQL 数据库** | `user_1` | `user_1` | `ROLE_USER` | 普通用户：仅可浏览产品列表 |
| **MySQL 数据库** | `editor_1` | `editor_1` | `ROLE_EDITOR` | 编辑角色：可浏览、添加、修改、删除产品 |
| **MySQL 数据库** | `adm_1` | `adm_1` | `ROLE_PRODUCT_ADMIN` | 超级管理员：拥有全部权限 |
| **LDAP 目录服务** | `ldap_user_1` | `ldap_user_1` | `ROLE_USER` | LDAP 普通用户：仅可浏览产品 |
| **LDAP 目录服务** | `ldap_editor_1` | `ldap_editor_1` | `ROLE_EDITOR` | LDAP 编辑角色：可浏览与增删改产品 |
| **LDAP 目录服务** | `ldap_adm_1` | `ldap_adm_1` | `ROLE_PRODUCT_ADMIN` | LDAP 管理员：拥有全量权限 |
| **GitHub OAuth2** | *(GitHub 用户)* | *(授权登录)* | **`ROLE_EDITOR`** | GitHub 登录成功后，系统固定分配 EDITOR 角色 |

---

## 💻 命令行 (CURL) 接口调用与实测输出验证

所有请求均经由统一对外开放端口 **`7573`** 通过 Gateway 转发。

### 1. 通过 CURL 获取 Access Token (密码模式)

#### A. 数据库普通用户 (`user_1`):
```bash
curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=user_1&password=user_1&client_id=demo_client&client_secret=demo_secret" \
  http://localhost:7573/uaa/token
```
**实测响应**：
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

#### B. LDAP 编辑用户 (`ldap_editor_1`):
```bash
curl -s -X POST -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=ldap_editor_1&password=ldap_editor_1&client_id=demo_client&client_secret=demo_secret" \
  http://localhost:7573/uaa/token
```
**实测响应**：
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

### 2. 浏览产品列表 (`GET /products`，需要 USER 及以上权限)

```bash
curl -s -H "Authorization: Bearer $USER_TOKEN" http://localhost:7573/products
```
**实测响应 (HTTP 200 OK)**：
```json
[
  {"id": 1, "name": "MacBook Pro 16"},
  {"id": 2, "name": "iPhone 15 Pro Max"},
  {"id": 3, "name": "iPad Pro M4"}
]
```

---

### 3. 添加产品 (`POST /products`，需要 EDITOR 及以上权限)

#### 使用 EDITOR 角色 Token 正常添加：
```bash
curl -s -X POST -H "Authorization: Bearer $EDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple Vision Pro"}' \
  http://localhost:7573/products
```
**实测响应 (HTTP 201 Created)**：
```json
{
  "id": 4,
  "name": "Apple Vision Pro"
}
```

#### 越权拦截：若使用 USER 角色 Token 尝试添加产品：
```bash
curl -s -i -X POST -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Illegal Item"}' \
  http://localhost:7573/products | head -n 1
```
**实测响应 (HTTP 403 Forbidden)**：
```text
HTTP/1.1 403 Forbidden
```

---

### 4. 修改产品 (`PUT /products/{id}`，需要 EDITOR 及以上权限)

```bash
curl -s -X PUT -H "Authorization: Bearer $EDITOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name": "Apple Vision Pro M2"}' \
  http://localhost:7573/products/4
```
**实测响应 (HTTP 200 OK)**：
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

### 5. 删除产品 (`DELETE /products/{id}`，需要 EDITOR 及以上权限)

```bash
curl -s -X DELETE -H "Authorization: Bearer $EDITOR_TOKEN" \
  http://localhost:7573/products/4
```
**实测响应 (HTTP 200 OK)**：
```json
{
  "message": "Product deleted successfully with id: 4"
}
```

---

## 📁 项目目录结构

```text
.
├── pom.xml                               # Maven 顶层依赖与聚合工程配置
├── docker-compose.yaml                   # 统一容器编排配置
├── README.md                             # 英文总览说明文档
├── README_ZH.md                          # 中文总览说明文档
├── REQUIREMENTS.md                       # 英文需求规格说明书
├── REQUIREMENTS_ZH.md                    # 中文需求规格说明书
├── ARCHITECTURE.md                       # 英文技术架构与底层实现设计
├── ARCHITECTURE_ZH.md                    # 中文技术架构与底层实现设计
├── start-local.sh                        # 本地后台一键启动脚本
├── stop-local.sh                         # 本地后台一键停止脚本
├── docker/
│   ├── mysql/init.sql                   # MySQL 自动建库建表与初始化脚本
│   └── ldap/bootstrap.ldif              # OpenLDAP 初始用户与组织结构
├── common/                               # 公共模块 (JWT 工具类与模型)
├── discovery-service/                    # Eureka 注册中心 (:8761)
├── config-service/                       # Spring Cloud Config 配置中心 (:8888)
├── gateway-service/                      # Spring Cloud Gateway 网关 (:7573，全系统唯一对外端口)
├── uaa-service/                          # UAA 用户认证中心 (:8081，三合一登录与 Token 签发)
└── product-service/                      # Product 产品服务 (:8082，角色继承 RBAC 鉴权)
```

---

## 📋 评审自测清单核验

| 验收项目 | 满足情况 | 验证说明 |
| :--- | :---: | :--- |
| **JDK 版本说明** | ✅ 已确认 | 明确标注并严格基于 **Java 17 (OpenJDK 17 LTS)** 编译开发 |
| **全模块成功编译打包** | ✅ 已确认 | `mvn clean install` 7 个模块全量通过，单元与集成测试全部成功 |
| **数据库自动初始化** | ✅ 已确认 | 自动创建 `microservices_db` 数据库、`users` 与 `products` 表 |
| **LDAP 目录服务初始化** | ✅ 已确认 | 挂载导入 `bootstrap.ldif`，预置 ldap_* 账号与权限组 |
| **单端口 7573 约束** | ✅ 已确认 | 仅网关映射宿主机 7573 端口，其他服务皆在 Docker 内网通信 |
| **角色层级模型实现** | ✅ 已确认 | 基于 Spring Security 6 落地 `PRODUCT_ADMIN > EDITOR > USER` |
| **CURL 闭环验证** | ✅ 已确认 | 提供包含 Token 签发、增删改查及 403 越权拦截的实测数据 |
