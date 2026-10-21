# 智校互联 (CampusHub) 校园二手交易协同平台
## 07. Docker 部署方案与 CI/CD 流水线

> **课程班级**：软件工程 2402 · 第 11 组  
> **制定人员**：边皓东（组长）  
> **生效版本**：v1.0.0（与 01 需求基线、02 API 契约、03 数据字典、06 测试用例配套）  
> **交付状态**：前端 / 后端 / 数据库三容器部署方案已落地并实测通过（三层回归 40 + 62 + 10 全绿）；CI 覆盖单元测试、前端构建、容器内集成与系统测试；CD 在打版本标签后构建并推送两个镜像，再用发布镜像真实部署回归

---

### 一、部署形态总览

平台采用**前端 / 后端 / 数据库三容器**部署，由 `docker-compose.yml` 统一编排，每个组件独立镜像、独立生命周期。

| 服务 | 镜像 | 容器名 | 端口映射 | 职责 |
| :--- | :--- | :--- | :--- | :--- |
| 前端 | 本地构建 `campushub-frontend` | `campushub-frontend` | `8080:80`（`APP_PORT` 可改） | Nginx 托管 Vite 构建产物，并把 `/api` 反向代理到后端 |
| 后端 | 本地构建 `campushub-backend` | `campushub-backend` | `8081:8080`（`BACKEND_PORT` 可改） | Spring Boot 提供 REST 接口 |
| 数据库 | `mysql:8.0` | `campushub-mysql` | `3307:3306`（`MYSQL_PORT` 可改） | 持久化数据，首次启动导入种子数据 |

**访问入口**：用户与浏览器统一走 `http://localhost:8080`（前端容器）；后端 8081 仅用于直连接口调试，前端容器通过 Docker 内部服务名 `backend:8080` 访问后端，不经过宿主机端口。

依赖顺序为 `mysql (healthy) → backend (healthy) → frontend`，由 compose 的 `depends_on.condition: service_healthy` 保证。

---

### 二、容器化设计

#### 2.1 前端镜像 `frontend/Dockerfile`

| 阶段 | 基础镜像 | 职责 |
| :--- | :--- | :--- |
| 构建 | `node:22-alpine` | `npm ci` 后执行 `npm run build -- --outDir dist` 生成静态产物 |
| 运行 | `nginx:1.27-alpine` | 托管 `/usr/share/nginx/html`，加载 `frontend/nginx.conf` |

`frontend/nginx.conf` 三件事：`try_files` 回退 `index.html` 支持前端路由；`location /api/` 反代到 `http://backend:8080` 并透传 `Host` / `X-Real-IP` / `X-Forwarded-For`；`/assets/` 静态资源长缓存。

> 仓库内 `vite.config.js` 的 `outDir` 指向后端静态目录（供本地开发与 `一键启动.bat` 使用），容器内通过 `--outDir dist` 覆盖为独立产物，两者互不影响。

#### 2.2 后端镜像 `backend/Dockerfile`

| 阶段 | 基础镜像 | 职责 |
| :--- | :--- | :--- |
| 构建 | `maven:3.9-eclipse-temurin-21` | 先 `dependency:go-offline` 预热依赖，再 `package` 打 jar |
| 运行 | `eclipse-temurin:21-jre` | 以非 root 用户 `appuser`（uid 1001）运行，暴露 8080 |

运行阶段带 `HEALTHCHECK`（`/dev/tcp` 探测 8080），时区固定 `Asia/Shanghai`，`JAVA_OPTS` 默认 `-XX:MaxRAMPercentage=75`。

#### 2.3 数据库容器

- `db/init.sql` 只读挂载到 `/docker-entrypoint-initdb.d/01-init.sql`，**仅在数据卷为空时执行一次**（14 用户 / 6 分类 / 32 商品 / 45 张图片 / 5 条砍价 / 1 张交易单）。
- 服务端强制 `utf8mb4`（`--character-set-server` / `--collation-server`）。
- **必须设置 `LANG=C.UTF-8`**：容器默认 `POSIX` 区域设置时，`mysql` 客户端会退化为 `latin1`，导入含中文的 SQL 会被二次编码成乱码。该问题在集成测试「卖家可读取私密会话内容」用例中暴露并修复。

#### 2.4 环境变量

以 `.env.example` 为模板复制为 `.env` 后调整（`.env` 已被 `.gitignore` 忽略，不入库）：

| 变量 | 默认值 | 作用 |
| :--- | :--- | :--- |
| `MYSQL_ROOT_PASSWORD` | `123456` | 数据库 root 密码，同时注入后端的 `SPRING_DATASOURCE_PASSWORD` |
| `APP_PORT` | `8080` | 前端容器映射到宿主机的端口 |
| `BACKEND_PORT` | `8081` | 后端容器映射到宿主机的端口（直连调试用） |
| `MYSQL_PORT` | `3307` | 数据库映射到宿主机的端口 |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75` | JVM 运行参数 |
| `MAVEN_MIRROR_URL` | 阿里云公共仓库 | **仅后端镜像构建期生效**，用于拉取 Maven 依赖；留空则走官方 Maven Central |
| `BACKEND_IMAGE` / `FRONTEND_IMAGE` | `campushub-backend:local` / `campushub-frontend:local` | 两个应用的镜像名，部署到目标机器时指向已发布镜像 |

---

### 三、部署与运维命令

```bash
# 首次部署
cp .env.example .env          # Windows: copy .env.example .env
docker compose up -d --build

# 日常运维
docker compose ps                       # 查看三个容器状态（含 healthy）
docker compose logs -f backend          # 后端日志（前端用 frontend，数据库用 mysql）
docker compose down                     # 停止并保留数据
docker compose down -v                  # 停止并清空数据库，下次启动重新导入种子数据
docker compose up -d --build frontend   # 仅重建前端镜像
```

部署就绪判定：三个容器均为 `healthy`，`http://localhost:8080/` 返回页面，`http://localhost:8080/api/categories` 返回 `code: 0`（即前端 → 后端链路通）。

> **环境依赖**：Windows 使用 Docker Desktop 的 WSL 2 后端，需启用 `Microsoft-Windows-Subsystem-Linux` 与 `VirtualMachinePlatform` 两项系统功能；Linux 服务器安装 Docker Engine + Compose 插件即可。

---

### 四、持续集成（CI）

配置文件 `.github/workflows/ci.yml`，对应 `docs/04` 的简化 Git Flow 分支模型。

**触发条件**：向 `main`、`develop`、`feature/**`、`release/**`、`hotfix/**` 推送；向 `main`、`develop` 发起 Pull Request；亦支持手动触发。同一分支的并发流水线自动取消旧运行。

| Job | 名称 | 覆盖层次 | 关键步骤 | 产物 |
| :--- | :--- | :--- | :--- | :--- |
| `unit-test` | 单元测试 | 单元测试 | JDK 21 + `mvn -B -f backend/pom.xml test` | Surefire 报告 |
| `frontend-build` | 前端构建校验 | 构建门禁 | `npm ci` + `npm run build`，校验产物生成 | — |
| `integration-system-test` | 集成测试 + 系统测试 | 集成测试 / 系统测试 | `docker compose up -d --build`（三容器）→ 等待前端入口就绪 → `pwsh tests/api-e2e.ps1` → Playwright 全量 UI 流程 | Playwright HTML 报告、失败时的容器日志 |

容器内两层测试直接跑在 CI 构建出的镜像上，验证的是「即将交付的制品」而不是开发机环境；接口经前端 Nginx 反向代理，等于顺带验证了代理链路。

---

### 五、持续部署（CD）

配置文件 `.github/workflows/cd.yml`。

**触发条件**：推送 `v*` 版本标签（如 `v1.0.0`），或手动触发。

| Job | 名称 | 关键动作 |
| :--- | :--- | :--- |
| `build-backend` | 构建并推送后端镜像 | 推送 `ghcr.io/<owner>/<repo>-backend`，标签含语义化版本、分支、短 SHA 与 `latest`（仅标签触发时） |
| `build-frontend` | 构建并推送前端镜像 | 推送 `ghcr.io/<owner>/<repo>-frontend`，标签同上 |
| `deploy-verify` | 拉取发布镜像并验证部署 | 按 **digest** 精确拉取两个镜像 → 起 MySQL 并等待种子导入完成 → 起后端 → 起前端 → 校验首页、经 Nginx 的接口与代理链路 → 对发布镜像执行 `tests/api-e2e.ps1` |

按 digest 而不是标签拉取，保证验证的就是本次流水线产出的那两个镜像层，不被后续覆盖的标签影响；两个镜像分别用 `scope=backend` / `scope=frontend` 隔离构建缓存。

**部署到目标机器**：

```bash
# bash
BACKEND_IMAGE=ghcr.io/<owner>/<repo>-backend:1.0.0 \
FRONTEND_IMAGE=ghcr.io/<owner>/<repo>-frontend:1.0.0 \
docker compose up -d --no-build

# PowerShell
$env:BACKEND_IMAGE='ghcr.io/<owner>/<repo>-backend:1.0.0'
$env:FRONTEND_IMAGE='ghcr.io/<owner>/<repo>-frontend:1.0.0'
docker compose up -d --no-build
```

两个镜像名同时写在 `docker-compose.yml` 的 `image:` 字段中，因此 `up --no-build` 会直接使用已发布镜像，不会在目标机器上重新构建。命令行传入的环境变量优先于 `.env` 文件中的值。

> GHCR 包默认为私有，目标机器需先 `docker login ghcr.io`；如需匿名拉取，在仓库 Packages 设置中改为 public。

---

### 六、测试分层与流水线映射

| 层次 | 载体 | 规模（截至 2026-10-21） | 触发位置 |
| :--- | :--- | :--- | :--- |
| 单元测试 | `backend/src/test`（JUnit 5 + Mockito） | 40 项 | CI `unit-test` |
| 集成测试 | `tests/api-e2e.ps1` 断言组 | 62 项断言 | CI `integration-system-test` |
| 集成测试（图形化） | Apifox 全量集合 / Newman | 96 请求 / 117 断言 | 迭代末手工回归 |
| 系统测试 | Playwright（`frontend/e2e`） | 10 条 UI 流程 | CI `integration-system-test` |
| 发布验证 | CD 流水线 | 冒烟 + 62 项接口断言 | CD `deploy-verify` |

用例编号、覆盖范围与手工复核要求见 `docs/06`；原始执行输出与截图留档见 `tests/reports/`。

---

### 七、代码质量分析（SonarQube）

测试报告回答「功能是否按预期跑通」，SonarQube 回答「代码是否有缺陷与隐患」，两者互补、不可互替。

**环境**：`docker-compose.sonar.yml` 单独编排 SonarQube 社区版 + PostgreSQL 16，**与交付用的三容器编排分离**，按需启动、互不影响：

```bash
docker compose -f docker-compose.sonar.yml up -d     # 起分析环境（http://localhost:9000）
docker compose -f docker-compose.sonar.yml down      # 停止，保留分析数据
```

**工程配置**：`backend/pom.xml` 引入 `jacoco-maven-plugin`（测试阶段采集覆盖率并生成 `target/site/jacoco/jacoco.xml`）与 `sonar-maven-plugin`，SonarQube 地址与项目标识通过 `<properties>` 提供，可用 `-Dsonar.host.url` / `-Dsonar.token` 覆盖。

**一键分析**：`pwsh -File tests\run-sonar.ps1` —— 自动完成「拉起容器 → 初始化管理员密码 → 生成一次性令牌 → 跑单测并采集覆盖率 → 上传分析 → 抓取指标留档」。令牌只用于当次上传，不写入任何文件。

**最近一次结果**（2026-10-21，详见 `tests/reports/2026-10-21-SonarQube代码质量报告.md`）：

| 指标 | 数值 |
| :--- | :--- |
| 质量门禁 | 通过（OK） |
| Bugs / Vulnerabilities / Code Smells | 0 / **1** / 42 |
| 覆盖率 | 27.7%（行 30.7% / 分支 22.2%） |
| 重复率 | 3.5% |
| 代码规模 | 2,926 行（ncloc） |

> 唯一的漏洞是 `java:S6437`：`JwtUtil` 中 JWT 签名密钥硬编码。该改动落在认证链路上（会使已签发令牌失效），需要产品侧确认后再处理，**当前未修改**；42 条代码异味全部是 `java:S6813`（Spring 字段注入）。

---

### 八、已知约束与排障

| 现象 | 原因 | 处理 |
| :--- | :--- | :--- |
| 构建阶段报 `Could not transfer ... from/to central` 超时 | 构建容器无法直连 Maven Central | 在 `.env` 中设置 `MAVEN_MIRROR_URL` 指向可用镜像源（默认已给阿里云） |
| 种子数据中文显示为乱码 | 数据库容器区域设置非 UTF-8，导入被二次编码 | 确认 compose 中 `LANG=C.UTF-8` 未被移除；已乱码的库执行 `docker compose down -v` 后重建 |
| 前端页面打开但接口 502 | 后端容器未就绪或已退出 | `docker compose logs backend` 查看启动日志；前端 `depends_on` 已等待后端 healthy，若手动重启后端，稍等其健康检查通过 |
| 宿主机 3306 / 8081 端口被占用 | 本机已有 MySQL 或其它服务 | 调整 `.env` 中的 `MYSQL_PORT` / `BACKEND_PORT` |
| 应用启动即退出 | 数据库未就绪或密码不匹配 | 先看 `docker compose logs mysql`，确认 `MYSQL_ROOT_PASSWORD` 与 `.env` 一致 |

**部署位置说明**：Windows 开发机上 Docker Desktop 程序目录安装在 `E:\Docker\Docker`，容器与镜像数据默认落在 `C:\Users\<用户>\AppData\Local\Docker\wsl`。
