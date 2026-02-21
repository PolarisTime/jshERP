# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

jshERP (管伊佳ERP) is an open-source ERP system for SMEs. It is a multi-module Maven project with:
- **jshERP-boot** — Spring Boot 2.0 backend (Java 8, port 9999)
- **jshERP-web** — Vue.js 2.7 frontend (port 3000)

## Build & Run Commands

### Backend (jshERP-boot)

```bash
# Build
mvn clean package

# Run
java -jar target/jshERP.jar

# Regenerate MyBatis mapper classes from DB schema
mvn mybatis-generator:generate
```

API docs available at: `http://localhost:9999/jshERP-boot/doc.html`

### Frontend (jshERP-web)

```bash
# Install dependencies
yarn install

# Development server (proxies /jshERP-boot/* to localhost:9999)
yarn serve

# Production build
yarn build
```

### Database Setup

```bash
# Initialize schema and seed data
mysql -u root -p123456 < jshERP-boot/docs/jsh_erp.sql
```

The file `jshERP-boot/docs/数据库更新记录.txt` tracks schema changes between versions (no Flyway/Liquibase).

## Prerequisites

- JDK 1.8
- Maven 3.3.9+
- Node.js 20+, Yarn
- MySQL 8.0 (database: `jsh_erp`, default credentials: root/123456)
- Redis 6.2 (default password: `1234abcd`)

## Architecture

### Backend Layer Structure

```
com.jsh.erp/
├── controller/     # REST controllers (30+) — thin layer, delegates to services
├── service/        # Business logic (31+) — transactions, validation, orchestration
├── datasource/
│   ├── entities/   # MyBatis-Plus entity classes (auto-generated)
│   ├── mappers/    # MyBatis mapper interfaces
│   └── vo/         # Value objects for complex query results
├── filter/         # Servlet filters (auth, tenant resolution)
├── config/         # Spring configuration beans
├── exception/      # Custom exceptions and global error handling
└── utils/          # Utility classes (30+)
```

MyBatis mapper SQL lives in `src/main/resources/mapper_xml/*.xml`. Entity classes are generated from DB using `generatorConfig.xml`.

### Frontend Layer Structure

```
src/
├── api/        # Axios API client modules (one file per backend module)
├── views/      # Page-level Vue components organized by business module
├── components/ # Reusable Vue components
├── store/      # Vuex state (auth, user session, permissions)
├── router/     # Vue Router (permission-gated routes in permission.js)
└── utils/      # Shared utilities (request interceptors, token, etc.)
```

### Key Design Patterns

- **Multi-tenancy**: Most DB tables have a `tenant_id` column. The filter layer injects the tenant context from the session. The `manage.roleId` config distinguishes platform admins from tenant users.
- **Plugin system**: Uses `springboot-plugin-framework 2.2.1`. Plugins live in `./plugins/` and are configured via `pluginConfig/`.
- **File storage**: Configurable via `file.uploadType` (1 = local at `file.path`, 2 = Aliyun OSS).
- **Session-based auth**: No JWT — sessions with a 10-hour timeout. The filter in `filter/` handles authentication and tenant isolation.

### Business Modules

Procurement, Sales, Inventory/Warehouse, Retail, Financial, Production, Reporting, Material Management, Personnel/Organization, System Administration.

## Configuration

Key settings in `jshERP-boot/src/main/resources/application.properties`:
- Server: port `9999`, context path `/jshERP-boot`
- DB: `jdbc:mysql://127.0.0.1:3306/jsh_erp`
- Redis: `127.0.0.1:6379`
- File upload path: `/opt/jshERP/upload`

Frontend proxy config in `jshERP-web/vue.config.js` — all `/jshERP-boot` requests are proxied to `http://localhost:9999`.

## Default Dev Credentials

- DB: `root` / `123456`
- Redis: password `1234abcd`
- App login: `admin` / `123456`
