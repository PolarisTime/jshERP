# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

jshERP (管伊佳ERP) is an open-source ERP system for SMEs. It is a multi-module Maven project with:
- **jshERP-boot** — Spring Boot 3 backend, JDK 17 baseline
- **jshERP-web** — Vue.js 2.7 frontend (port 3000)

Current branch assumptions:
- The legacy multi-tenant runtime has been removed. This is now a single-organization deployment.
- The plugin subsystem has been removed from the active code path.
- Local dev typically runs the backend on port `10099` against the `jsh_erp_steel_dev` database.

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

API docs are exposed at `/jshERP-boot/doc.html`. In local dev this is commonly `http://localhost:10099/jshERP-boot/doc.html`.

### Frontend (jshERP-web)

```bash
# Install dependencies
yarn install

# Development server (proxies /jshERP-boot/* to localhost:10099)
yarn serve

# Production build
yarn build
```

### Database Setup

```bash
# Initialize schema and seed data
mysql -u root -p123456 < jshERP-boot/docs/jsh_erp.sql
```

The file `jshERP-boot/docs/数据库更新记录-首次安装请勿使用.txt` is a historical upgrade ledger. It contains legacy tenant-era SQL and is not the authoritative schema for the current single-organization deployment.

## Prerequisites

- JDK 17
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
├── filter/         # Servlet filters (auth, request preprocessing)
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

- **Single-organization deployment**: The legacy isolation layer has been removed. Business isolation is now handled by organization, permissions, and user-scoped data where applicable.
- **No plugin runtime**: Historical plugin files may still appear in old documents, but the active backend/frontend no longer provide plugin management.
- **File storage**: Configurable via `file.uploadType` (1 = local at `file.path`, 2 = Aliyun OSS).
- **Session-based auth**: No JWT — sessions with a 10-hour timeout. The filter in `filter/` handles authentication and request checks.

### Business Modules

Procurement, Sales, Inventory/Warehouse, Retail, Financial, Production, Reporting, Material Management, Personnel/Organization, System Administration.

## Configuration

Key settings are in `jshERP-boot/src/main/resources/application.yml`:
- Server: default port `10099`, context path `/jshERP-boot`
- DB: current dev default is `jdbc:mysql://127.0.0.1:3306/jsh_erp_steel_dev`
- Redis: current dev default is `127.0.0.1:16379`
- File upload path: `/instance/jsh_steel/uploads`

Frontend proxy config in `jshERP-web/vue.config.js` — local `/jshERP-boot` requests are proxied to `http://localhost:10099`.

## Default Dev Credentials

- DB: `root` / `123456`
- Redis: password `1234abcd`
- App login: `steel` / `123456`
