# 测试结果留档目录

本目录保存**可追溯的测试执行证据**，与 `docs/06` 的执行结果章节配套：`docs/06` 记录方案与结论，这里存放产生结论的原始输出。

## 目录结构

```
tests/reports/
├─ README.md                    本说明
├─ <日期>-全量回归记录.md        当次回归的人读版记录（结论、环境、缺陷）
├─ raw/
│   ├─ <日期>-environment.txt   环境快照（分支 / 提交 / JDK / 镜像 ID / 种子数据）
│   ├─ <日期>-unit.txt          单元测试原始输出
│   ├─ <日期>-api-e2e.txt       接口 E2E 脚本原始输出
│   ├─ <日期>-apifox-newman.txt Apifox 集合 Newman 执行原始输出
│   ├─ <日期>-playwright.txt    Playwright 原始输出
│   ├─ <日期>-sonarqube-scan.txt       SonarQube 扫描原始日志
│   ├─ <日期>-sonarqube-metrics.json   代码质量指标（Web API 原始响应）
│   └─ <日期>-sonarqube-qualitygate.json 质量门禁判定
└─ screenshots/<日期>/         界面留档：8 张页面截图（.jpg）+ 2 张 SonarQube 质量看板（.png）
```

> Apifox 图形界面导出的场景报告（HTML 与结果页截图）与集合放在一起，位于 `tests/apifox/`，本目录的回归记录会链接引用。

## 如何执行

```powershell
# 1. 三层回归（单元 + 集成 + 系统），自动写入 raw/
pwsh -File tests\run-regression.ps1

# 2. Apifox 集合（Newman 无头执行）
npx newman run tests/apifox/campushub-apifox.postman_collection.json `
  -e tests/apifox/campushub-apifox.environment.json --reporters cli --color off

# 3. 关键页面截图留档
cd frontend; node scripts/capture-screenshots.mjs

# 4. 代码质量分析（SonarQube 自托管）
docker compose -f docker-compose.sonar.yml up -d
pwsh -File tests\run-sonar.ps1
cd frontend; $env:SONAR_PASSWORD='<管理员密码>'; node scripts/capture-sonar-dashboard.mjs
```

`run-regression.ps1` 的退出码非 0 即表示存在失败层次，可直接用于本地门禁。

### 关于 PowerShell 脚本的执行

1. **两个主机都能用**：`pwsh`（PowerShell 7，CI 与本机默认）和 `powershell`（Windows PowerShell 5.1）均已实测通过；5.1 下建议显式加 `-ExecutionPolicy Bypass`，避免执行策略拦截：

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File tests\run-regression.ps1
   ```

2. **`.ps1` 必须存为 UTF-8 带 BOM**。Windows PowerShell 5.1 对无 BOM 的文件按系统代码页（简体中文为 GBK）解析，中文提示与比较会失效；`api-e2e.ps1` 与 `run-regression.ps1` 均已带 BOM，编辑时请勿用会丢 BOM 的工具覆盖保存。

3. **执行位置无关**：脚本内部用 `$PSScriptRoot` 定位 `api-e2e.ps1` 与 `backend/pom.xml`，在任意目录执行都可以。

4. **前置条件**：后端需已在 `http://localhost:8080` 可访问（脚本会先预检，失败则退出码 2）；未安装或未启动 Docker 时不影响执行，只是环境快照里会记录「未取到」种子数据。

## 约定

1. **原始输出不得手工修改**，只做追加与归档；结论写进 `<日期>-全量回归记录.md`，不要改 raw 里的文件；
2. 每次迭代末（或发布前）至少留档一份，文件名带日期，不覆盖历史记录；
3. 执行前请确认目标后端可用、数据库为预期的种子状态——`environment.txt` 会记录回归前的种子数据量，便于回溯；
4. 接口 E2E 与 UI 系统测试会写入真实数据，回归结束后建议 `docker compose down -v` 恢复干净种子；
5. CI（`.github/workflows/ci.yml`）会把 Surefire 报告与 Playwright HTML 报告作为流水线产物上传，与本目录的手工留档互为补充。
