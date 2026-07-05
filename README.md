<div align="center">

# 🏃 CampusRunner — Campus Errand & Purchasing System

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.11-green?logo=springboot&logoColor=white">
  <img alt="Vue" src="https://img.shields.io/badge/Vue-3.4-brightgreen?logo=vuedotjs&logoColor=white">
  <img alt="Vite" src="https://img.shields.io/badge/Vite-5-646CFF?logo=vite&logoColor=white">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white">
  <img alt="MyBatis-Plus" src="https://img.shields.io/badge/MyBatis--Plus-3.5.7-red">
  <img alt="License" src="https://img.shields.io/badge/License-Academic-blue">
</p>

> A campus-scoped errand-running & purchasing service platform (NOT a second-hand trading platform).
> Core business table: `errand_order` · Database: `campus_runner`

📖 **中文文档请见 [README_zh.md](./README_zh.md)**

</div>

---

## 📖 Overview

**CampusRunner** is a full-stack web application built for university campuses. It connects students who need errands run (food pickup, parcel collection, item purchasing) with student runners willing to fulfill them for a small fee. The system covers the complete lifecycle of an errand order: **publish → grab → deliver → confirm → rate**.

- **Backend**: Spring Boot 3 + MyBatis-Plus, exposing a RESTful API with a unified `Result` envelope.
- **Frontend**: Vue 3 + Vite + Element Plus + Pinia + Vue Router, an SPA with token-based auth.
- **Persistence**: MySQL 8 (`utf8mb4`), logical-delete aware.

---

## ✨ Feature Modules

| Module | Description |
| --- | --- |
| 👤 User | Registration / login (BCrypt-hashed password, JWT-style token), 3 roles: `0`-client, `1`-runner, `2`-admin |
| 📦 Errand Order | Publish order, browse order hall, grab order, mark delivered, confirm completion, cancel |
| ⭐ Order Rate | Client rates runner (score 1–5 + comment) after completion |
| 🖼️ Upload | Voucher / receipt image upload, exposed via static resource mapping |
| 📚 API Docs | Knife4j (OpenAPI 3) interactive docs at `/doc.html` |

### Order State Machine

```
0 待接单 (pending)  ──grab──▶  1 配送中 (delivering)  ──deliver──▶  2 已送达 (delivered)
        │                                                                     │
        │                                                              ──confirm──▶  3 已完成 (finished)
        └──────────────────────────────cancel────────────────────▶  4 已取消 (cancelled)
```

---

## 🧱 Tech Stack

**Backend**
- Spring Boot 3.5.11 · Java 21
- MyBatis-Plus 3.5.7 (auto-fill, logical delete, pagination)
- MySQL Connector/J · spring-security-crypto (BCrypt)
- Knife4j 4.5.0 + springdoc-openapi 2.6.0
- Lombok · spring-boot-starter-validation

**Frontend**
- Vue 3.4 (Composition API) · Vite 5
- Element Plus 2.7 · Pinia 2 · Vue Router 4
- Axios 1.6 (unified interceptors)

---

## 📁 Project Structure

```
CampusRunner/
├── src/main/java/com/campus/runner/
│   ├── CampusRunnerApplication.java      # Boot entrypoint
│   ├── common/                            # Result, DTOs, error codes
│   ├── config/                            # CORS, MyBatisPlus, MetaObjectHandler, Security
│   ├── controller/                        # UserController, ErrandOrderController, UploadController, TestController
│   ├── entity/                            # User, ErrandOrder, OrderRate
│   ├── mapper/                            # MyBatis-Plus mappers
│   ├── service/                           # Service interfaces + impl
│   └── exception/                         # Global exception handler
├── src/main/resources/
│   ├── application.yml                    # Common config, active=prod
│   ├── application-dev.yml                # Dev datasource (local)
│   ├── application-prod.yml               # Prod datasource (env-placeholder externalized)
│   └── schema.sql                         # DDL (CREATE TABLE IF NOT EXISTS)
├── campus-runner-frontend/                # Vue 3 SPA
│   ├── src/{api,views,router,utils}/
│   ├── .env.development                   # VITE_API_BASE_URL = http://localhost:8080
│   ├── .env.production                    # VITE_API_BASE_URL = /api
│   └── vite.config.js                     # Dev proxy /api -> 8080
├── pom.xml
└── delivery_products/                     # Build deliverables
```

---

## 🗄️ Database Design

| Table | Purpose |
| --- | --- |
| `user` | Accounts (username, BCrypt password, nickname, role) |
| `errand_order` | **Core table** — errand orders with client/runner, addresses, fees, status, logical delete |
| `order_rate` | Client ratings for completed orders |

Init script: [`campus_runner.sql`](./campus_runner.sql) (idempotent — safe to re-run).

```sql
CREATE DATABASE campus_runner DEFAULT CHARACTER SET utf8mb4;
mysql -u root -p campus_runner < campus_runner.sql
```

---

## 🚀 Quick Start

### Prerequisites
- JDK 21 · MySQL 8 · Node.js 18+ (only for rebuilding frontend)

### 1. Database
```bash
mysql -u root -p < campus_runner.sql
```

### 2. Backend
```bash
# Dev profile (local DB, password hardcoded in application-dev.yml)
java -jar campus-runner-backend.jar --spring.profiles.active=dev

# Prod profile (override password externally — recommended for production)
java -jar campus-runner-backend.jar \
     --spring.profiles.active=prod \
     --spring.datasource.password=YOUR_PROD_PASSWORD
```
Backend listens on `:8080`. API docs: `http://localhost:8080/doc.html`

### 3. Frontend (already built in `dist/`)
Serve `dist/` with any static server, or use Nginx reverse proxy (`/api/` → `:8080`).
See [`README_zh.md`](./README_zh.md) §部署 for a complete Nginx sample.

---

## 🔌 API Sneak Peek

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/user/register` | Register |
| POST | `/user/login` | Login, returns token |
| GET | `/errand/order/page` | Paginated order hall |
| POST | `/errand/order` | Publish errand order |
| POST | `/test/order` | (Test) insert a sample order |
| GET | `/test/order` | (Test) list all orders |

All responses follow the unified envelope:
```json
{ "code": 200, "message": "success", "data": { ... } }
```

---

## 🔐 Security Notes

- Production DB credentials are **externalized** via `${DB_URL}` / `${DB_USER}` / `${DB_PWD}` placeholders — never hardcoded.
- Prefer environment variables over command-line flags for the password (avoids process-list / shell-history leakage).
- CORS is configured for `http://localhost:*` and `http://127.0.0.1:*` (dev). Tighten origins for production.

---

## 📜 License

Academic / training project. © CampusRunner, 2026.

<div align="center"><sub>Built as a comprehensive web-dev training deliverable · 2026.07</sub></div>
