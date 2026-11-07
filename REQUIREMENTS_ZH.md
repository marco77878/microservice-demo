# 📋 Spring Cloud 微服务全栈平台 - 需求规格说明书

---

## 一、 项目背景

Spring Cloud 是由 Pivotal 团队提供的一整套成熟的微服务开源解决方案，涵盖了服务注册与发现、配置中心、全链路监控、服务网关、负载均衡以及熔断断路器等核心组件。

本项目旨在基于 Spring Cloud 最新稳定版本落地一套包含统一认证中心（UAA）、API 网关、产品业务服务、配置与服务发现的完整微服务系统，并实现基于 Docker Compose 的一键化容器编排交付。

---

## 二、 架构与服务组成需求

### 1. 容器编排要求
必须提供一个统一的 `docker-compose.yaml`，实现以下服务的一键式拉起与自愈运行：

| 服务名称 | 核心职责与功能说明 | 外部端口约束 |
| :--- | :--- | :--- |
| **Gateway (网关)** | API 统一网关，负责全链路鉴权路由与全局 Token 透传 | **仅此服务对外暴露端口 `7573` (HTTP，不用 HTTPS)** |
| **Uaa (认证中心)** | 统一身份认证：支持数据库密码、LDAP 目录与 GitHub OAuth2 登录 | 仅容器内网访问，不对外映射端口 |
| **Product (产品服务)** | 产品业务服务：维护产品表（自增主键 ID + 产品名 Name）并进行角色权限控制 | 仅容器内网访问，不对外映射端口 |
| **Discovery (注册中心)** | 微服务注册与发现（如 Eureka Server） | 仅容器内网访问，不对外映射端口 |
| **Configure (配置中心)** | 集中化配置管理中心（如 Spring Cloud Config） | 仅容器内网访问，不对外映射端口 |
| **MySQL (关系数据库)** | 存储用户数据与业务产品数据（容器启动需自动建库建表与初始化） | 仅容器内网访问，不对外映射端口 |
| **LDAP (目录服务)** | 企业目录服务（OpenLDAP），预置初始组织节点与用户凭据 | 仅容器内网访问，不对外映射端口 |

> **架构选型注记**：若选型组件本身可承担多个服务角色，允许合理合并或扩展，核心需满足上述所有微服务职责与单一 7573 端口约束。

---

## 三、 身份认证与登录规范 (UAA)

### 1. 登录 Web 控制台
- **访问地址**：必须通过网关端口 `7573` 访问（例如 `http://localhost:7573/login`）。
- **页面交互**：界面样式可从简，但必须支持**点击或切换**以下三种登录方式：
  1. 数据库账号密码登录
  2. LDAP 目录账号登录
  3. GitHub OAuth2 授权登录

### 2. 预置测试账号矩阵

| 认证源 | 用户名 | 密码 | 预分配角色 | 权限说明 |
| :--- | :--- | :--- | :--- | :--- |
| **MySQL (数据库)** | `user_1` | `user_1` | `ROLE_USER` | 普通用户：仅允许浏览产品 |
| **MySQL (数据库)** | `editor_1` | `editor_1` | `ROLE_EDITOR` | 编辑角色：可浏览并增删改产品 |
| **MySQL (数据库)** | `adm_1` | `adm_1` | `ROLE_PRODUCT_ADMIN` | 超级管理员：拥有全部权限 |
| **LDAP (目录服务)** | `ldap_user_1` | `ldap_user_1` | `ROLE_USER` | LDAP 普通用户：仅允许浏览产品 |
| **LDAP (目录服务)** | `ldap_editor_1` | `ldap_editor_1` | `ROLE_EDITOR` | LDAP 编辑角色：可浏览并增删改产品 |
| **LDAP (目录服务)** | `ldap_adm_1` | `ldap_adm_1` | `ROLE_PRODUCT_ADMIN` | LDAP 管理员：拥有全部权限 |
| **GitHub OAuth2** | *(GitHub 用户)* | *(授权登录)* | **`ROLE_EDITOR`** | 登录成功后，系统固定赋予 EDITOR 角色 |

---

## 四、 角色层级模型与接口权限控制 (RBAC)

### 1. 角色层级继承树 (Role Hierarchy)
系统角色权限严格遵循包含继承逻辑：

> **`PRODUCT_ADMIN` > `EDITOR` > `USER`**

- **`PRODUCT_ADMIN`**：拥有 `EDITOR` 与 `USER` 的所有权限；
- **`EDITOR`**：拥有 `USER` 的只读浏览权限，以及新增、修改、删除权限；
- **`USER`**：基础只读角色，仅拥有产品浏览权限。

### 2. Product 业务接口鉴权规则

| 请求类型 | 接口路径 | 最低角色要求 | 允许访问的角色 | 鉴权失败预期响应 |
| :--- | :--- | :--- | :--- | :--- |
| **GET** | `/products` | `USER` | `USER`, `EDITOR`, `PRODUCT_ADMIN` | `401 Unauthorized` |
| **POST** | `/products` | `EDITOR` | `EDITOR`, `PRODUCT_ADMIN` | `403 Forbidden` |
| **PUT** | `/products/{id}` | `EDITOR` | `EDITOR`, `PRODUCT_ADMIN` | `403 Forbidden` |
| **DELETE** | `/products/{id}` | `EDITOR` | `EDITOR`, `PRODUCT_ADMIN` | `403 Forbidden` |

---

## 五、 技术栈与交付要求

1. **框架与环境**：
   - **框架**：Spring Boot（最新稳定版，如 3.x）+ Spring Cloud（2022.x/2023.x）
   - **构建管理**：Apache Maven（多模块工程）
   - **开发语言**：Java（必须在 README 中明确声明所需的 JDK 版本，推荐 JDK 17）
2. **交付形态**：
   - 源码工程附带标准 `docker-compose.yaml` 及配套 Dockerfile。
   - 具备完整运行文档的 `README.md`，包含实际可执行的 Bash CURL 验证命令与真实返回日志。

---

## 六、 评审与自测验收清单

### 1. 评审组验证流程
评审人员将在目标服务器上执行以下标准流程，**配置文件修改不得超过 10 行**：
```bash
# 1. 获取或 Pull 源码
git pull

# 2. 编译并打包所有微服务
mvn clean install

# 3. 编排并拉起所有容器服务
docker-compose up
```

### 2. 自测验收确认项 Checklist

- [x] **JDK 版本标注**：`README.md` 中清晰声明了项目所需的 JDK 版本 (Java 17)。
- [x] **源码编译成功**：执行 `mvn clean install` 全模块构建成功（BUILD SUCCESS）。
- [x] **数据库自初始化**：启动容器后能自动创建对应数据库、数据表及初始账号。
- [x] **目录服务自初始化**：LDAP 服务正常拉起，预置用户及组信息成功载入。
- [x] **服务正常启动与注册**：网关、UAA、Product、Eureka 等微服务均正常启动且互联就绪。
- [x] **端口暴露合规**：确认全系统**仅网关向外开放 7573 端口**。
- [x] **CURL 验证闭环**：通过 README 提供的 CURL 命令行能稳定复现以下输出：
  1. 通过 CURL 获取 Access Token（密码模式传参）；
  2. 携带 Token 调用浏览产品 API；
  3. 携带 Token 调用添加产品 API；
  4. 携带 Token 调用修改产品 API；
  5. 携带 Token 调用删除产品 API；
  6. 普通用户尝试添加产品时被正确拦截为 `403 Forbidden`。
