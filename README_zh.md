<div align="center">

# 🏃 CampusRunner · 校园跑腿代购系统

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.11-green?logo=springboot&logoColor=white">
  <img alt="Vue" src="https://img.shields.io/badge/Vue-3.4-brightgreen?logo=vuedotjs&logoColor=white">
  <img alt="Vite" src="https://img.shields.io/badge/Vite-5-646CFF?logo=vite&logoColor=white">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white">
  <img alt="MyBatis-Plus" src="https://img.shields.io/badge/MyBatis--Plus-3.5.7-red">
  <img alt="License" src="https://img.shields.io/badge/License-教学实训-blue">
</p>

> 面向高校校园场景的跑腿代购服务平台（**绝非二手交易平台**）
> 核心业务表：`errand_order` · 数据库名：`campus_runner`

📖 **English version: [README.md](./README.md)**

</div>

---

## 📖 项目简介

**CampusRunner（校园跑腿代购系统）** 是一套面向高校校园的全栈 Web 应用，连接「需要跑腿的同学」与「愿意接单赚取跑腿费的同学」。系统覆盖跑腿订单的完整生命周期：**发布 → 抢单 → 配送 → 送达 → 确认 → 评价**。

- **后端**：Spring Boot 3 + MyBatis-Plus，提供 RESTful 接口，统一 `Result` 返回结构。
- **前端**：Vue 3 + Vite + Element Plus + Pinia + Vue Router，单页应用，基于 Token 鉴权。
- **持久层**：MySQL 8（`utf8mb4` 字符集），支持逻辑删除。

---

## ✨ 功能模块

| 模块 | 说明 |
| --- | --- |
| 👤 用户 | 注册 / 登录（BCrypt 加密密码，JWT 风格 Token），三种角色：`0`-发单同学、`1`-跑腿骑手、`2`-管理员 |
| 📦 跑腿订单 | 发布订单、浏览订单大厅、抢单、标记送达、确认完成、取消订单 |
| ⭐ 订单评价 | 完成后发单同学对骑手评分（1–5 分 + 文字评价） |
| 🖼️ 文件上传 | 跑腿凭证 / 小票图片上传，通过静态资源映射对外暴露 |
| 📚 接口文档 | Knife4j（OpenAPI 3）交互式文档，访问 `/doc.html` |

### 订单状态机

```
0 待接单  ──抢单──▶  1 配送中  ──送达──▶  2 已送达
   │                                       │
   │                                ──确认完成──▶  3 已完成
   └─────────────────取消─────────▶  4 已取消
```

---

## 🧱 技术栈

**后端**
- Spring Boot 3.5.11 · Java 21
- MyBatis-Plus 3.5.7（自动填充、逻辑删除、分页）
- MySQL Connector/J · spring-security-crypto（BCrypt）
- Knife4j 4.5.0 + springdoc-openapi 2.6.0
- Lombok · spring-boot-starter-validation

**前端**
- Vue 3.4（Composition API）· Vite 5
- Element Plus 2.7 · Pinia 2 · Vue Router 4
- Axios 1.6（统一拦截器）

---

## 📁 项目结构

```
CampusRunner/
├── src/main/java/com/campus/runner/
│   ├── CampusRunnerApplication.java      # 启动入口
│   ├── common/                            # Result、DTO、错误码
│   ├── config/                            # CORS、MyBatisPlus、MetaObjectHandler、Security
│   ├── controller/                        # UserController、ErrandOrderController、UploadController、TestController
│   ├── entity/                            # User、ErrandOrder、OrderRate
│   ├── mapper/                            # MyBatis-Plus Mapper
│   ├── service/                           # Service 接口与实现
│   └── exception/                         # 全局异常处理
├── src/main/resources/
│   ├── application.yml                    # 公共配置，默认 active=prod
│   ├── application-dev.yml                # 开发环境数据源（本地）
│   ├── application-prod.yml               # 生产环境数据源（占位符外置）
│   └── schema.sql                         # 建表脚本（CREATE TABLE IF NOT EXISTS）
├── campus-runner-frontend/                # Vue 3 单页应用
│   ├── src/{api,views,router,utils}/
│   ├── .env.development                   # VITE_API_BASE_URL = http://localhost:8080
│   ├── .env.production                    # VITE_API_BASE_URL = /api
│   └── vite.config.js                     # 开发代理 /api -> 8080
├── pom.xml
└── delivery_products/                     # 构建交付物
```

---

## 🗄️ 数据库设计

| 表名 | 用途 |
| --- | --- |
| `user` | 用户账号（用户名、BCrypt 密码、昵称、角色） |
| `errand_order` | **核心表** —— 跑腿订单（发单人/接单骑手、地址、费用、状态、逻辑删除） |
| `order_rate` | 已完成订单的评价 |

初始化脚本：[`campus_runner.sql`](./campus_runner.sql)（幂等，可安全重复执行）。

```sql
CREATE DATABASE campus_runner DEFAULT CHARACTER SET utf8mb4;
mysql -u root -p campus_runner < campus_runner.sql
```

---

## 🚀 快速开始

### 环境要求
- JDK 21 · MySQL 8 · Node.js 18+（仅二次构建前端时需要）

### 1. 初始化数据库
```bash
mysql -u root -p < campus_runner.sql
```

### 2. 启动后端
```bash
# 开发环境（连本地库，密码写在 application-dev.yml）
java -jar campus-runner-backend.jar --spring.profiles.active=dev

# 生产环境（外置覆盖密码 —— 生产推荐）
java -jar campus-runner-backend.jar \
     --spring.profiles.active=prod \
     --spring.datasource.password=你的生产密码
```
后端监听 `:8080`。接口文档：`http://localhost:8080/doc.html`

### 3. 前端（已构建在 `dist/` 内）
用任意静态服务器托管 `dist/`，或使用 Nginx 反向代理（`/api/` → `:8080`）。Nginx 配置示例见下文「部署」。

---

## 🌐 前端部署（Nginx 反向代理示例）

前端生产环境 `VITE_API_BASE_URL = '/api'`，所有接口以 `/api` 开头，由 Nginx 反代到后端 8080，浏览器同源、彻底规避跨域。

```nginx
server {
    listen       80;
    server_name  your.domain.com;

    root   /usr/share/nginx/campus-runner;   # dist/ 解压到此目录
    index  index.html;

    # SPA 历史模式：刷新子路径回退到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理：去掉 /api 前缀转发给后端
    #   前端请求 /api/errand/order/list  ->  后端收到 /errand/order/list
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 50m;
    }
}
```

```bash
nginx -t && nginx -s reload
```

---

## 🔌 接口预览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/user/register` | 用户注册 |
| POST | `/user/login` | 登录，返回 Token |
| GET | `/errand/order/page` | 订单大厅分页查询 |
| POST | `/errand/order` | 发布跑腿订单 |
| POST | `/test/order` | （测试）插入一条示例订单 |
| GET | `/test/order` | （测试）查询全部订单 |

所有接口统一返回结构：
```json
{ "code": 200, "message": "success", "data": { ... } }
```

---

## 🔐 安全说明

- 生产数据库密码**外置化**：通过 `${DB_URL}` / `${DB_USER}` / `${DB_PWD}` 占位符设计，绝不硬编码进仓库。
- 生产密码优先用环境变量（`export DB_PWD=xxx`）传入，避免出现在进程列表 / Shell 历史中。
- CORS 当前放行 `http://localhost:*` 与 `http://127.0.0.1:*`（开发期），生产环境请收紧来源域名。

---

## 📜 许可

教学 / 实训项目。© CampusRunner, 2026.

<div align="center"><sub>Web 开发技术综合实训 · 期末终结性评价交付物 · 2026.07</sub></div>
