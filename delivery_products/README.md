<div align="center">

# 🏃 CampusRunner · 校园跑腿代购系统
### 生产上线部署说明文档（期末终结性评价交付物）

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.11-green?logo=springboot&logoColor=white">
  <img alt="Vue" src="https://img.shields.io/badge/Vue-3.4-brightgreen?logo=vuedotjs&logoColor=white">
  <img alt="Vite" src="https://img.shields.io/badge/Vite-5-646CFF?logo=vite&logoColor=white">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white">
  <img alt="MyBatis-Plus" src="https://img.shields.io/badge/MyBatis--Plus-3.5.7-red">
</p>

> **核心定位**：面向高校校园场景的跑腿代购服务系统（非二手交易平台），核心业务表为 `errand_order`，数据库名 `campus_runner`。

</div>

---

## 📑 目录

- [一、交付物清单](#一交付物清单)
- [二、运行环境要求](#二运行环境要求)
- [三、数据库初始化](#三数据库初始化)
- [四、后端部署（Jar 包外置参数启动）](#四后端部署jar-包外置参数启动)
- [五、前端部署（Nginx 反向代理）](#五前端部署nginx-反向代理)
- [六、多环境配置说明](#六多环境配置说明)
- [七、常见问题排查（FAQ）](#七常见问题排查faq)
- [八、安全加固建议](#八安全加固建议)

---

## 一、交付物清单

本目录 `delivery_products/` 内含以下文件：

| 文件 / 目录 | 说明 |
| --- | --- |
| `campus-runner-backend.jar` | 后端 Spring Boot 可执行 Fat-Jar（含全部依赖，约 37MB） |
| `campus-runner-frontend-dist/` | 前端 Vue 3 生产构建产物（纯静态资源：`index.html` + `assets/`） |
| `campus_runner.sql` | 数据库初始化建表脚本（`CREATE TABLE IF NOT EXISTS`，幂等可重复执行） |
| `README.md` | 本文档 |

---

## 二、运行环境要求

| 组件 | 最低版本 | 推荐版本 | 备注 |
| --- | --- | --- | --- |
| JDK | 21 | 21 LTS | 后端 Jar 强依赖 Java 21 |
| MySQL | 8.0 | 8.0+ | 字符集 `utf8mb4` |
| Node.js | 18 | 20+ | 仅二次构建时需要，部署无需 |
| Nginx | 1.20 | 1.24+ | 前端静态托管 + 反向代理 |
| OS | Linux x86_64 | Ubuntu 22.04 / CentOS 8+ | Windows 亦可，命令等价 |

---

## 三、数据库初始化

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 创建数据库（utf8mb4 支持完整 Emoji 与生僻字）
CREATE DATABASE IF NOT EXISTS campus_runner
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

# 3. 退出后执行建表脚本（脚本内为 CREATE TABLE IF NOT EXISTS，可安全重复执行）
mysql -u root -p campus_runner < campus_runner.sql
```

> 📌 **核心表**：`user`（用户）、`errand_order`（跑腿订单，系统核心表）、`order_rate`（订单评价）。
> 📌 **状态机**：订单 `status` —— `0`待接单 `1`配送中 `2`已送达 `3`已完成 `4`已取消。
> 📌 **角色**：用户 `role` —— `0`发单同学 `1`跑腿骑手 `2`管理员。

---

## 四、后端部署（Jar 包外置参数启动）

### ✅ 最简启动

```bash
java -jar campus-runner-backend.jar
```

> 默认激活 `prod` 生产环境（已在 `application.yml` 中声明 `spring.profiles.active: prod`），并使用内置兜底配置连接 `127.0.0.1:3306` 的 `campus_runner`。

### 🔐 推荐：动态覆盖密码启动（生产必备）

生产环境的数据库密码 **绝不硬编码进仓库**。通过命令行参数动态覆盖：

```bash
java -jar campus-runner-backend.jar \
     --spring.profiles.active=prod \
     --spring.datasource.password=你的生产密码
```

### 🎛️ 全量外置参数（URL / 用户名 / 密码均可覆盖）

`application-prod.yml` 已设计为占位符外置化，支持三种覆盖方式，优先级 **命令行 > 环境变量 > 默认值**：

```bash
# 方式 A：命令行参数（最直观，但密码会出现在进程列表，不建议用于生产密码）
java -jar campus-runner-backend.jar \
     --spring.profiles.active=prod \
     --spring.datasource.url='jdbc:mysql://10.0.0.8:3306/campus_runner?serverTimezone=Asia/Shanghai&characterEncoding=utf8' \
     --spring.datasource.username=runner_app \
     --spring.datasource.password=你的生产密码

# 方式 B：环境变量（推荐！密码不进进程列表，更安全）
export DB_URL='jdbc:mysql://10.0.0.8:3306/campus_runner?serverTimezone=Asia/Shanghai&characterEncoding=utf8'
export DB_USER='runner_app'
export DB_PWD='你的生产密码'
java -jar campus-runner-backend.jar --spring.profiles.active=prod

# 方式 C：混合使用 —— URL/用户名走命令行，密码走环境变量（兼顾可读性与安全）
export DB_PWD='你的生产密码'
java -jar campus-runner-backend.jar \
     --spring.profiles.active=prod \
     --spring.datasource.url='jdbc:mysql://10.0.0.8:3306/campus_runner?serverTimezone=Asia/Shanghai&characterEncoding=utf8' \
     --spring.datasource.username=runner_app
```

### 🚀 后台常驻运行（nohup）

```bash
nohup java -jar campus-runner-backend.jar \
     --spring.profiles.active=prod \
     --spring.datasource.password=你的生产密码 \
     > campus-runner.log 2>&1 &

# 查看实时日志
tail -f campus-runner.log
```

启动成功后，后端监听 **8080** 端口，接口文档地址：`http://服务器IP:8080/doc.html`（Knife4j）。

---

## 五、前端部署（Nginx 反向代理）

### 5.1 上传静态资源

将 `campus-runner-frontend-dist/` 目录整体上传至服务器，例如 `/usr/share/nginx/campus-runner`。

### 5.2 Nginx 配置

前端生产环境 `VITE_API_BASE_URL = '/api'`，所有接口请求以 `/api` 开头，由 Nginx 反向代理到后端 8080，**浏览器同源、彻底规避跨域**。

```nginx
server {
    listen       80;
    server_name  your.domain.com;        # 替换为你的域名或服务器 IP

    # 前端静态资源根目录
    root   /usr/share/nginx/campus-runner;
    index  index.html;

    # SPA 路由历史模式：刷新子路径时回退到 index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理：/api/ 前缀转发至后端，并去掉 /api 前缀
    #   前端请求  /api/errand/order/list
    #   后端收到  /errand/order/list
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # 上传文件场景，允许的请求体大小需略大于后端限制，避免 Nginx 先行拦截
        client_max_body_size 5m;
    }
}
```

```bash
# 校验配置语法
nginx -t
# 平滑重载
nginx -s reload
```

访问 `http://your.domain.com` 即可使用。

---

## 六、多环境配置说明

后端采用 Spring Boot 多 Profile 设计，配置文件拆分如下：

| 文件 | 作用 | 数据源策略 |
| --- | --- | --- |
| `application.yml` | 公共配置（端口、MyBatis-Plus、SpringDoc 等），全局 `active: prod` | — |
| `application-dev.yml` | 开发环境 | 直连 `localhost:3306`，账号 `root` / 密码 `a1234567` 写死 |
| `application-prod.yml` | 生产环境 | 占位符 `${DB_URL}` / `${DB_USER}` / `${DB_PWD}` 外置覆盖 |

**本地开发**切换为 dev：

```bash
java -jar campus-runner-backend.jar --spring.profiles.active=dev
```

或在 IDE 启动配置的 Active profiles 中填 `dev`。

前端环境变量（Vite）：

| 文件 | `VITE_API_BASE_URL` | 用途 |
| --- | --- | --- |
| `.env.development` | `http://localhost:8080` | 开发直连后端，配合后端 CORS |
| `.env.production` | `/api` | 生产走 Nginx 反向代理 |

`src/utils/request.js` 通过 `import.meta.env.VITE_API_BASE_URL` 动态读取。

---

## 七、常见问题排查（FAQ）

**Q1：启动报错 `Access denied for user 'root'`？**
密码不匹配。确认命令行 `--spring.datasource.password` 或环境变量 `DB_PWD` 是否与生产库实际密码一致。

**Q2：启动报错 `Communications link failure`？**
数据库地址/端口不通。检查 `--spring.datasource.url` 或 `DB_URL` 是否指向可达的 MySQL，以及 3306 端口防火墙是否放行。

**Q3：前端页面白屏 / 接口 404？**
Nginx 的 `location /api/` 未正确 `proxy_pass` 到后端。注意 `proxy_pass http://127.0.0.1:8080/;` 末尾的 `/` 会剥离 `/api` 前缀；若后端接口本身带 `/api` 前缀则去掉末尾斜杠。

**Q4：刷新子路由 404？**
Nginx 缺少 `try_files $uri $uri/ /index.html;`，SPA 历史模式回退未配置。

**Q5：上传图片失败，提示 413？**
Nginx `client_max_body_size` 过小，调至 `5m`（略大于后端 `max-request-size` 2MB，确保 Nginx 不先于后端拦截）。后端已限制单文件 / 请求体上限为 2MB 以防 OOM。

---

## 八、安全加固建议

1. **生产库账号最小权限**：勿用 `root`，新建专用账号仅授权 `campus_runner.*` 的 `SELECT/INSERT/UPDATE/DELETE`。
2. **密码走环境变量**：优先 `export DB_PWD=xxx`，避免出现在进程命令行 / shell 历史。
3. **关闭接口文档**：生产可在启动参数追加 `--springdoc.api-docs.enabled=false --springdoc.swagger-ui.enabled=false`。
4. **HTTPS**：Nginx 配置 SSL 证书，启用 443，HTTP 80 强制跳转。
5. **防火墙**：仅开放 80/443，后端 8080 与 MySQL 3306 不对外暴露。

---

<div align="center">

<sub>📚 CampusRunner 实训项目 · 期末终结性评价交付物 · 2026.07</sub>

</div>
