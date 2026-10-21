# 智校互联 (CampusHub) 校园二手交易协同平台

> **《软件工程项目实现》课程设计 · 软件工程 2402 · 第 11 组**  
> **技术栈**：Spring Boot 3 + Vue 3 (Vite 工程化) + MySQL 8.0 + Docker Compose（零外部中间件开箱即用体系，技术决策见 `docs/01`）  
> **当前状态**：三天计划全部完成——Day 1 ~ Day 3 的 8 个成员提交均已合并 `develop`，12 个用户故事全部落地；自动化测试：后端 JUnit 92 项、Apifox 全量集合 96 请求 / 117 断言（Apifox 场景 96 步全绿、未测 0）、Playwright UI 10 条流程，接口脚本 `tests/api-e2e.ps1` 65 项断言；已完成容器化部署与 GitHub Actions CI/CD 流水线（详见 `docs/07`）。收尾优化与交易闭环重构已完成（进度见 `docs/05` 第四节）。

---

## 一、项目说明

Sprint 1~3 共 12 个用户故事 / 52 故事点，按业务域划分为：用户模块（认证）、商品模块（发布 / 检索 / 详情 / 管理）、交易与互动模块（砍价 / 留言互动 / 交易单与意向预约）、平台门户模块（首页数据看板）、信用与治理模块（评价举报 / 管理后台）与工程交付线（一键启动脚本）。

已实现的主要能力：

- 学号注册与登录，密码 BCrypt 单向哈希存储，注册自动生成内联 SVG 头像；
- 集市商品列表：关键词 / 分类 / 成色 / 校区筛选，四种排序，分页，登录态自动排除自己发布的商品；
- 商品详情：多图切换、立省金额、卖家名片（不公开手机号，站内联系）、浏览量累加；
- 闲置发布：表单校验、图片魔数校验、Base64 Data URI 直存 MySQL、发布后跳转详情；
- 我的发布：状态筛选、下架 / 重新上架 / 线下已售，含越权与非法流转校验；
- 我的购买：交易单按状态筛选 + 待成交 / 已成交 / 待评价 / 累计支出统计，附在谈的出价与预约，可一键联系卖家；
- 商品问答：公开留言板，买家提问、卖家回复、未读提醒（匿名可浏览）；
- 交易单私密会话：买卖双方可约定面交时间地点，非交易方不可读写，与公开的商品问答完全分开；
- 砍价：出价需低于挂牌价且不低于挂牌价 8 折（弹窗展示可出价区间与 9 / 8.5 / 8 折快捷出价，前后端双重校验）；
- 预约购买：按挂牌价提交意向（含期望面交时间与备注），卖家确认后生成交易单并锁定商品；
- 信用与治理：成交双方互评（信用分 = 基础 80 分 ± 评价）、违规商品举报、管理员处置留痕；
- 管理端：与用户端分开的独立入口与独立布局，用户 / 商品 / 举报管理；管理员账号不参与用户侧交易；
- 分类在售数量、平台统计（在售 / 成交 / 认证用户 / 累计浏览）接口。

需求、接口契约、数据模型、协作规范与测试设计均已完成文档化，见下节索引。

---

## 二、当前进度

| 用户故事 | 状态 | 说明 |
| :--- | :---: | :--- |
| US-01 学号注册与登录 | 已实现 | 注册 / 登录 / 退出、BCrypt 密码哈希、JWT 签发与登录拦截 |
| US-02 集市多维检索 | 已实现 | 筛选、排序、分页、分类计数、登录态排除自己 |
| US-03 商品详情与卖家联系 | 已实现 | 多图画廊、立省金额、卖家名片（手机号不公开，未成交走商品问答、成交后走交易单私聊）、浏览量 +1 |
| US-04 闲置发布与图片直存 | 已实现 | 发布表单、图片上传与 Base64 直存 |
| US-05 我的发布与状态流转 | 已实现 | 列表、状态流转；待成交期间禁止手动改状态，手动售出按「线下已售」处理 |
| US-06 砍价议价闭环 | 已实现 | 出价 / 接受 / 拒绝 / 取消；接受出价生成交易单 |
| US-07 一键启动脚本 | 已实现 | 一键启停、建库脚本与 E2E 回归；另有 Docker Compose 容器化部署与 GitHub Actions CI/CD 流水线（见 `docs/07`） |
| US-08 平台数据看板 | 已实现 | 统计接口与首页四项指标看板 |
| US-09 站内留言互动 | 已实现 | 商品留言、回复与未读提醒 |
| US-10 交易单与线下交易预约 | 已实现 | 立即购买 / 砍价成交 / 预约购买三路径统一交易单（预约按挂牌价、卖家确认后锁定），确认成交、取消解锁与「我的购买」 |
| US-11 信用评价与举报 | 已实现 | 交易评价与违规举报 |
| US-12 管理后台 | 已实现 | 独立登录页 `/#/admin/login`；用户 / 商品 / 举报管理；管理员不参与用户交易 |

> 状态口径：`已实现` 表示接口与页面已跑通并通过手工验收，正式判定为「已完成」以 `docs/06` 用例执行通过为准。

---

## 三、目录结构

```text
campushub/
├─ backend/     Spring Boot 3 后端（Controller / Service / Mapper / MyBatis XML）
├─ frontend/    Vue 3 + Vite 前端（集市 / 发布 / 详情 / 交易中心 / 我的购买 / 我的发布 / 信用 / 管理端）
├─ db/          建库脚本与演示种子数据（db/init.sql）
└─ docs/        需求、接口契约、数据库设计、协作规范、分工与测试用例
```

---

## 四、本地运行

### 1. 环境要求

- JDK 21
- Maven 3.9+
- Node.js 20+
- MySQL 8.0（默认 `localhost:3306`）

### 2. 初始化数据库

在项目根目录执行（会重建 `campushub` 库并导入演示数据）：

```bash
mysql -uroot -p --default-character-set=utf8mb4 -e "source db/init.sql"
```

脚本内置 14 个用户（含管理员 `admin`）、6 个分类、32 个商品、45 张商品图片、5 条砍价记录、1 张已完成交易单与 3 条交易单私聊。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。数据库账号密码在 `backend/src/main/resources/application.yml` 中，按本机 MySQL 配置修改即可。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，Vite 已把 `/api` 代理到后端 8080 端口。

### 5. 演示账号

| 角色 | 账号 | 密码 |
| :--- | :--- | :--- |
| 学生（组长） | `542412320201` | `123456` |
| 管理员 | `admin` | `123456` |

> **管理后台**：与用户端分开，固定入口 `/#/admin/login`（即 `http://localhost:8080/#/admin/login`）。该页只接受上表的**管理员账号**，学生账号会被拒绝；**管理员账号不参与用户侧活动**——不能发布、出价、预约、留言、评价、举报，只能只读浏览集市与使用后台管理，用户端也不会显示这些操作入口。登录后左侧为「数据概览 / 用户管理 / 商品管理 / 举报处置」，退出登录回到该登录页。

---

---

## 五、Docker 一键运行

项目按**前端 / 后端 / 数据库三容器**部署，由 `docker-compose.yml` 统一编排，首次启动自动导入 `db/init.sql` 演示数据：

| 服务 | 镜像 | 容器名 | 宿主机端口 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 前端 | `frontend/Dockerfile`（Node 构建 → Nginx） | `campushub-frontend` | `8080` | 托管静态页面，并把 `/api` 反代到后端容器 |
| 后端 | `backend/Dockerfile`（Maven 构建 → JRE 21） | `campushub-backend` | `8081` | Spring Boot 接口服务，前端经服务名 `backend:8080` 访问 |
| 数据库 | `mysql:8.0` | `campushub-mysql` | `3307` | 数据持久化，卷为空时导入种子数据 |

> 完整的部署设计、环境变量说明与排障表见 [07. Docker 部署方案与 CI/CD 流水线](docs/07_Docker部署与CICD流水线.md)。

### 1. 环境要求

- Docker Desktop（Windows 使用 WSL 2 后端）
- 首次安装后至少手动启动一次 Docker Desktop，待引擎状态变为 Running

### 2. 启动

```bash
copy .env.example .env      # macOS / Linux 用 cp
docker compose up -d --build
```

启动完成后访问 `http://localhost:8080`（管理端 `http://localhost:8080/#/admin/login`），演示账号同「四、本地运行」第 5 节。后端接口可用 `http://localhost:8081/api/...` 直连调试。

> 三个容器按 `mysql (healthy) → backend (healthy) → frontend` 顺序启动。容器内 MySQL 在宿主机映射为 **3307**（`MYSQL_PORT`），避免与本机已有的 3306 冲突。`db/init.sql` 只在数据卷为空时执行一次。

### 3. 常用命令

| 目的 | 命令 |
| :--- | :--- |
| 查看状态 / 日志 | `docker compose ps` / `docker compose logs -f backend`（可换 `frontend` / `mysql`） |
| 停止（保留数据） | `docker compose down` |
| 清空数据并重新导入 | `docker compose down -v` 后再 `docker compose up -d` |
| 仅重建前端 / 后端镜像 | `docker compose up -d --build frontend`（后端换成 `backend`） |

### 4. 可配置环境变量

见 `.env.example`：`MYSQL_ROOT_PASSWORD`、`MYSQL_PORT`、`APP_PORT`、`BACKEND_PORT`、`JAVA_OPTS`、`MAVEN_MIRROR_URL`、`BACKEND_IMAGE`、`FRONTEND_IMAGE`。

> `MAVEN_MIRROR_URL` 只在镜像构建阶段生效，用于拉取后端 Maven 依赖，默认指向阿里云公共仓库，留空则走官方 Maven Central。

---

## 六、已实现接口

| 模块 | 方法 | 路径 | 说明 |
| :--- | :---: | :--- | :--- |
| 认证 | POST | `/api/auth/register` | 学号注册（自动登录并生成头像） |
| | POST | `/api/auth/login` | 学号登录 |
| | GET | `/api/auth/me` | 当前登录用户信息 |
| | POST | `/api/auth/logout` | 退出登录 |
| 商品 | GET | `/api/items` | 集市列表（筛选 / 排序 / 分页，登录态排除自己） |
| | GET | `/api/items/{id}` | 商品详情（浏览量 +1；含 `bargainFloor` 砍价下限） |
| | GET | `/api/items/mine` | 我的发布（状态筛选 / 分页） |
| | POST | `/api/items` | 发布商品 |
| | PATCH | `/api/items/{id}/status` | 商品状态流转（在售 / 下架 / 线下已售，仅本人） |
| | POST | `/api/files/upload` | 图片上传（文件头魔数校验，返回 Data URI） |
| | GET | `/api/categories` | 分类列表与在售数量 |
| | GET | `/api/system/stats` | 平台统计（在售 / 成交 / 认证用户 / 累计浏览） |
| 留言互动 | GET / POST | `/api/items/{itemId}/messages` | 商品问答列表（匿名可读）/ 发表留言与回复 |
| | GET | `/api/messages/mine` | 我的留言（收到 / 发出，含未读数） |
| | PATCH | `/api/messages/{id}/read`、`/api/messages/item/{itemId}/read` | 标记单条 / 整条商品留言为已读 |
| 交易 | POST / GET | `/api/bargains` | 提交出价（≥ 挂牌价 8 折且 < 挂牌价）/ 砍价列表 |
| | POST | `/api/bargains/{id}/accept\|reject\|cancel` | 接受（生成交易单）/ 拒绝 / 取消出价 |
| | POST / GET | `/api/reservations` | 提交预约购买意向 / 预约列表 |
| | POST | `/api/reservations/{id}/confirm\|reject\|cancel` | 确认（生成交易单并锁定）/ 拒绝 / 取消预约 |
| | POST | `/api/deals/direct` | 立即购买（按挂牌价生成待成交交易单） |
| | GET | `/api/deals` | 我的交易单（收到 / 发出，含待成交数与私聊未读数） |
| | POST | `/api/deals/{id}/complete\|cancel` | 确认成交 / 取消交易单 |
| | GET / POST | `/api/deals/{id}/messages` | 交易单私密会话（仅买卖双方可见，与公开问答分开） |
| 信用与治理 | POST | `/api/reviews` | 交易完成后对交易对手方评价 |
| | GET | `/api/users/{userId}/reviews` | 用户信用分与收到的评价 |
| | GET | `/api/reviews/mine` | 我发出的评价 + 待我评价的成交 |
| | POST | `/api/reports` | 举报违规商品 |
| | GET | `/api/reports/mine` | 我的举报与处理结果 |
| 管理端（仅 ADMIN） | GET | `/api/admin/users` | 用户列表 |
| | PATCH | `/api/admin/users/{id}/status` | 启用 / 禁用账号 |
| | GET | `/api/admin/items` | 全部商品（含下架 / 已售） |
| | PATCH | `/api/admin/items/{id}/off-shelf` | 强制下架违规商品（会同步取消待成交交易单） |
| | GET | `/api/admin/reports` | 举报队列 |
| | PATCH | `/api/admin/reports/{id}` | 举报处置（下架 / 驳回） |

完整请求参数、响应结构与业务码见 [02. RESTful API 接口契约](docs/02_RESTful_API接口契约.md)。除匿名接口外均需在请求头携带 `Authorization: Bearer <token>`，前端已统一注入；未登录访问受保护接口返回 HTTP 401。**管理员账号**只允许认证接口、`/api/admin/**` 与只读浏览，其余用户侧写接口一律返回 `4003`。

---

## 七、一键脚本与测试

- `一键启动.bat`：六步自检（JDK 21 → MySQL80 服务 → 数据库结构与自动导入 → 前端产物构建 → 后端打包 → 启动并打开浏览器）；
- `一键停止.bat`：释放 8080 端口并关闭后端进程；
- `db\init-db.bat`：一键重建演示数据（14 用户 / 6 分类 / 32 商品 / 45 图 / 5 条砍价 / 1 张交易单 / 3 条交易单私聊）；
- `tests\api-e2e.ps1`：接口 E2E 回归（65 项断言），覆盖认证、鉴权、检索、详情（含手机号不外泄）、留言、砍价 8 折下限、预约、交易单三路径成交与取消、交易单私聊权限、评价举报、举报处置后强制下架、管理后台与「管理员不参与用户交易」的关键断言。
- `tests\run-regression.ps1`：一键串起单元 / 集成 / 系统三层回归。PowerShell 7 用 `pwsh -File tests\run-regression.ps1`，Windows PowerShell 5.1 用 `powershell -NoProfile -ExecutionPolicy Bypass -File tests\run-regression.ps1`；原始输出自动留档到 `tests\reports\raw\`，退出码非 0 即表示有失败层次。
- `tests\run-sonar.ps1`：代码质量分析（SonarQube 自托管）。先 `docker compose -f docker-compose.sonar.yml up -d` 起分析环境，再执行脚本，自动跑测试、采集 JaCoCo 覆盖率、上传分析并抓取质量指标留档。

**三层自动化测试**

| 层 | 命令 / 入口 | 覆盖 |
| :--- | :--- | :--- |
| 后端单元测试 | `cd backend && mvn test` | JUnit 5 + Mockito，92 项：JWT 签发/防篡改/过期、注册登录与密码升级、站内留言与未读、预约全状态、管理端用户/商品/举报处置、砍价 8 折边界、交易单状态机、私聊权限、评价资格、商品隐私与状态流转、管理员隔离 |
| API（Apifox） | 导入 [tests/apifox](tests/apifox/README.md) 下的全量集合与环境 | 96 个请求 / 117 条断言：认证、商品、留言、砍价、预约、交易单、私聊、评价举报、管理端与权限；Apifox 场景 96 步全绿、未测 0（其中 2 条 401 用例关闭场景默认响应码校验只留断言，1 条图片上传用例手动挂接文件） |
| UI（Playwright） | `cd frontend && npm run test:ui` | 10 条流程：买家下单→确认成交→我的购买、管理员独立入口、管理员用户端登录拦截、砍价下限弹窗校验、集市搜索与筛选、发布闲置弹窗、商品问答、预约购买、我的购买联系卖家私聊、信用中心 |

> Playwright 默认使用系统 Edge（`channel: 'msedge'`），无需额外下载浏览器；CI 中设 `PW_CHANNEL=bundled` 改用 Playwright 自带 Chromium。执行前需有可用后端，配置会自动复用已启动的 8080 服务。Apifox 集合同样可用 Newman 无头执行：`npx newman run tests/apifox/campushub-apifox.postman_collection.json -e tests/apifox/campushub-apifox.environment.json`。

**测试与质量留档**：原始执行输出、环境快照、8 张页面截图与 2 张 SonarQube 质量看板存放于 [tests/reports](tests/reports/README.md)，当次汇总见 [2026-10-21 全量回归记录](tests/reports/2026-10-21-全量回归记录.md)；代码质量结果见 [2026-10-21 SonarQube 代码质量报告](tests/reports/2026-10-21-SonarQube代码质量报告.md)。

**持续集成与持续部署**

- `.github/workflows/ci.yml`：推送或发起 PR 时自动执行「后端单元测试 → 前端构建校验 → 容器内集成测试与系统测试」三级门禁，留存 Surefire 报告与 Playwright HTML 报告；
- `.github/workflows/cd.yml`：推送 `v*` 标签时构建**前端与后端两个镜像**并推送 GHCR，随后按 digest 逐个拉取、真实部署三容器、冒烟（含 Nginx 反代链路）并执行接口回归。

> 容器化设计、环境变量、运维命令与流水线细节见 [07. Docker 部署方案与 CI/CD 流水线](docs/07_Docker部署与CICD流水线.md)。

脚本细节：JDK 检查优先读 `%JAVA_HOME%\bin\java.exe`（PATH 上的 java 可能是旧版本）；启动后轮询 8080 端口最多 90 秒，就绪才打开浏览器；端口被占用时提示先执行停止脚本；重建数据库前会提示后端正在运行。三个 `.bat` 均为 ASCII + CRLF，避免 cmd 解析乱码。

## 八、团队

本项目由软件工程 2402 第 11 组 6 人团队承接，分工采用**功能模块 × 前后端协作**模式：每个交付单元由前端、后端各 1 名主责人共同完成；成员名单与分工表见 [05. 团队组织与功能模块分工](docs/05_团队组织与功能模块分工.md)。

---

## 九、文档索引

1. **[01. 需求规格说明书与用户故事](docs/01_需求规格说明书与用户故事.md)**：包含愿景目标、用户画像、12 个用户故事（52 SP）的 Product Backlog、MoSCoW 优先级与 59 条验收标准 (AC)；
2. **[02. 前后端 RESTful API 接口契约说明书](docs/02_RESTful_API接口契约.md)**：包含统一响应体、业务码表、JWT 鉴权、权限矩阵（含管理员隔离）与全模块接口契约（认证 / 商品 / 留言 / 砍价 / 预约 / 交易单与私聊 / 评价举报 / 管理后台）；
3. **[03. 数据库设计与数据字典规范](docs/03_数据库设计与数据字典.md)**：包含 E-R 关系图、11 张表数据字典、索引策略、关键事务一致性与 MySQL 8.0 完整 DDL；
4. **[04. 敏捷协作与 Git 工作流规范](docs/04_敏捷协作与Git工作流规范.md)**：包含迭代节奏、看板与会议机制、Git Flow 分支策略、Conventional Commits 与代码评审规则；
5. **[05. 团队组织与功能模块分工](docs/05_团队组织与功能模块分工.md)**：用户 / 商品 / 交易与互动 / 平台门户 / 信用与治理 5 个功能模块 + 工程交付线的模块负责制分工表；
6. **[06. 测试用例与执行规范](docs/06_测试用例与执行规范.md)**：包含按 M1~M6 功能模块组织的 63 条测试用例（接口 / UI / 脚本分层）、执行时机、留档规范与截至 2026-10-21 的执行结果；
7. **[07. Docker 部署方案与 CI/CD 流水线](docs/07_Docker部署与CICD流水线.md)**：包含前端 / 后端 / 数据库的容器化设计、环境变量与运维命令、CI 三层测试门禁、CD 发布验证流程，以及自托管 SonarQube 代码质量分析。
