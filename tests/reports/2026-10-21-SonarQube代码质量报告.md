# 2026-10-21 SonarQube 代码质量报告

> 本文件为**事实留档**：所有指标均来自自托管 SonarQube 的 Web API，原始响应见 `raw/2026-10-21-sonarqube-metrics.json` 与 `raw/2026-10-21-sonarqube-qualitygate.json`，扫描日志见 `raw/2026-10-21-sonarqube-scan.txt`。

## 一、分析环境

| 项 | 值 |
| :--- | :--- |
| SonarQube | 社区版 `sonarqube:community`（26.10.0.133078），自托管于 `http://localhost:9000` |
| 依赖数据库 | PostgreSQL 16（`docker-compose.sonar.yml` 内的 `sonar-db` 服务） |
| 扫描器 | `sonar-maven-plugin` 5.8.0.7211 |
| 覆盖率采集 | JaCoCo 0.8.12（`jacoco-maven-plugin`，测试阶段生成 `target/site/jacoco/jacoco.xml`） |
| 分析对象 | `campushub-backend`（后端模块，Java 21） |
| 分析时间 | 2026-10-21（分析用时约 1.8 分钟，含 40 项单测执行） |
| 镜像留档 | [`screenshots/2026-10-21/09-SonarQube-代码质量总览.png`](screenshots/2026-10-21/09-SonarQube-代码质量总览.png)、[`10-SonarQube-问题清单.png`](screenshots/2026-10-21/10-SonarQube-问题清单.png) |

## 二、总体结果

**质量门禁：通过（OK）**，代码规模 2,926 行（ncloc）。

| 指标 | 数值 | 评级 / 说明 |
| :--- | :--- | :--- |
| Bugs | 0 | Reliability 评级 C（见下方说明） |
| Vulnerabilities | **1** | 安全评级 **E** —— 硬编码密钥，需处理 |
| Security Hotspots | 0 | — |
| Code Smells | 42 | 可维护性评级 A，技术债约 210 分钟（3.5 小时） |
| 覆盖率 Coverage | 27.7% | 行覆盖 30.7%，分支覆盖 22.2% |
| 重复率 Duplications | 3.5% | 低于常见阈值 5% |
| 复杂度 | 圈复杂度 731 / 认知复杂度 353 | — |

> 仪表盘上「Reliability 42 open issues」与「Maintainability 42 open issues」是同一批 42 个代码异味在两类质量属性下的呈现（新版 Clean Code 分类下，一条问题可同时影响多个属性），不是 84 个问题。

## 三、问题清单

### 3.1 漏洞（BLOCKER，1 条）

| 规则 | 位置 | 问题 | 处置建议 |
| :--- | :--- | :--- | :--- |
| `java:S6437` | `backend/src/main/java/com/campushub/common/JwtUtil.java:69`（密钥声明在同文件第 17 行） | JWT 签名密钥硬编码在源码里：`private static final String SECRET = "campushub-jwt-secret-2026";`，扫描器在使用处（`new SecretKeySpec(SECRET...)`）判定 | 把密钥外置到 `application.yml` 并支持环境变量覆盖（如 `JWT_SECRET`），仓库内只保留开发用默认值，生产环境通过环境变量注入；密钥轮换后需通知全体用户重新登录 |

> **状态：暂未修改。** 这条落在认证链路上，改动会影响已签发的 JWT，需要产品侧确认后再动，不在本轮「搭建代码质量分析」的范围内。

### 3.2 代码异味（42 条，均为同一规则）

| 规则 | 数量 | 说明 | 处置建议 |
| :--- | :--- | :--- | :--- |
| `java:S6813` | 42 | Field dependency injection should be avoided —— 使用 `@Autowired` 字段注入 | 改为构造器注入（字段声明为 `private final`，由 Spring 构造器装配），便于单测直接 new 出对象、也能暴露循环依赖 |

## 四、覆盖率说明

覆盖率 27.7% 是**后端模块**的口径，需要说明清楚避免误读：

- 40 项 JUnit 用例聚焦在业务规则与状态机（砍价 8 折下限、交易单三路径与状态流转、私聊权限、评价资格、管理员隔离），对这些分支的覆盖是充分的；
- 未被覆盖的主要是 Controller、DTO、配置类与启动装配代码——它们由集成测试（`tests/api-e2e.ps1` 62 项断言、Apifox 96 请求 / 117 断言）与系统测试（Playwright 10 条流程）从外部覆盖，只是这些外部测试的执行不会体现在 SonarQube 的 Java 覆盖率数字里。

若要把这部分也计入，需要引入 JaCoCo 的集成测试采集（`jacoco-agent` 挂到运行中的服务）后再上传，属于后续可选项。

## 五、已知告警

扫描日志中有一条 `Missing blame information for the following files: pom.xml`——原因是 `backend/pom.xml` 本次有未提交改动，`git blame` 取不到对应修订。**提交后该告警会自动消失**，不影响指标与质量门禁。

## 六、复现方式

```powershell
# 1. 起 SonarQube 与 PostgreSQL（首次约 1 分钟，镜像约 2.5 GB）
docker compose -f docker-compose.sonar.yml up -d

# 2. 一键分析：初始化凭据 → 生成令牌 → 跑测试与覆盖率 → 上传 → 抓取指标留档
pwsh -File tests\run-sonar.ps1

# 3. 仪表盘
#    http://localhost:9000/dashboard?id=campushub-backend   账号 admin / Campushub-Sonar-2026

# 4. 截图留档
cd frontend
$env:SONAR_PASSWORD='Campushub-Sonar-2026'
node scripts/capture-sonar-dashboard.mjs
```

> 分析令牌由脚本每次临时生成，只用于当次上传，不写入任何文件或仓库；管理员密码为本地自托管实例的约定值，不属于仓库机密。

## 七、后续建议

1. **优先修 `java:S6437`**：把 JWT 密钥外置，这是唯一影响安全评级的项；
2. 批量改构造器注入（`java:S6813`×42），可结合 IDE 重构一次性处理；
3. 逐步把 Controller 层纳入单元测试，或把集成测试覆盖率接入 JaCoCo，让覆盖率数字更贴近真实；
4. 若要把代码质量也纳入门禁，可在 CI 中加一个可选 job：起 SonarQube 容器 → `mvn sonar:sonar`，并对 `qualitygates/project_status` 做断言。
