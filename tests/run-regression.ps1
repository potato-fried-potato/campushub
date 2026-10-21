# CampusHub 三层回归一键执行与留档
# 单元测试（JUnit）→ 集成测试（接口 E2E）→ 系统测试（Playwright）
# 用法：pwsh -File tests\run-regression.ps1
param(
    [string]$BaseUrl   = 'http://localhost:8080',
    [string]$ReportDir = (Join-Path $PSScriptRoot 'reports\raw'),
    [switch]$SkipUnit
)

# 用 Continue：本脚本要驱动 java / mvn / npm / docker 等会把提示写到 stderr 的命令，
# Windows PowerShell 5.1 在 Stop 模式下会把这类 stderr 输出当成终止错误。成功与否统一看退出码。
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$stamp    = Get-Date -Format 'yyyy-MM-dd'
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

function Save-Report {
    param([string]$Suffix, $Lines)
    $target = Join-Path $ReportDir "$stamp-$Suffix.txt"
    $Lines | Set-Content -LiteralPath $target -Encoding utf8
    Write-Host "    留档：$target" -ForegroundColor DarkGray
}

# ---------- 0. 环境信息 ----------
$envLines = @()
$envLines += "时间：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
$envLines += "主机：$env:COMPUTERNAME / $([System.Environment]::OSVersion.VersionString)"
$envLines += "分支：$(git -C $repoRoot rev-parse --abbrev-ref HEAD)  提交：$(git -C $repoRoot rev-parse --short HEAD)"
$envLines += "工作区未提交改动数：$((git -C $repoRoot status --porcelain | Measure-Object).Count)"
$envLines += "应用地址：$BaseUrl"
$javaHome = $env:JAVA_HOME
if ($javaHome -and (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
    $envLines += "JAVA_HOME：$javaHome"
    $envLines += "Maven 实际使用的 java：$((& (Join-Path $javaHome 'bin\java.exe') -version 2>&1 | Select-Object -First 1))"
}
else {
    $envLines += "未设置 JAVA_HOME，PATH java：$((& java -version 2>&1 | Select-Object -First 1))"
}
$envLines += "node：$(& node -v)"

if (Get-Command docker -ErrorAction SilentlyContinue) {
    $dockerVersion = & docker version --format '{{.Server.Version}}' 2>$null
    $envLines += "docker server：$dockerVersion"
    $images = & docker images --format '{{.Repository}}:{{.Tag}} {{.ID}}' 2>$null | Select-String 'campushub'
    foreach ($image in $images) { $envLines += "镜像：$image" }
    $dbImage = & docker ps --format '{{.Image}}' 2>$null | Select-String 'mysql'
    foreach ($item in $dbImage) { $envLines += "数据库容器镜像：$item" }
}

# 预检：后端必须可用，否则三层测试全部无意义
try {
    $probe = Invoke-WebRequest -Uri "$BaseUrl/api/categories" -UseBasicParsing -TimeoutSec 10
    $envLines += "预检：接口存活（HTTP $($probe.StatusCode)）"
}
catch {
    $envLines += "预检：失败（$($_.Exception.Message)）"
    Save-Report 'environment' $envLines
    Write-Host "后端 $BaseUrl 不可用，已写入预检记录，终止回归" -ForegroundColor Red
    exit 2
}

$seedSql = "SELECT CONCAT('user=',(SELECT COUNT(*) FROM campushub.user)," +
           "' item=',(SELECT COUNT(*) FROM campushub.item)," +
           "' image=',(SELECT COUNT(*) FROM campushub.item_image)," +
           "' bargain=',(SELECT COUNT(*) FROM campushub.bargain)," +
           "' deal=',(SELECT COUNT(*) FROM campushub.deal)) FROM DUAL;"
$seed = $null
if (Get-Command docker -ErrorAction SilentlyContinue) {
    $seed = & docker exec campushub-mysql mysql -uroot -p123456 -N -B -e $seedSql 2>$null
    if ($LASTEXITCODE -ne 0) { $seed = $null }
}
if ($seed) {
    $envLines += "数据库种子（回归前）：$seed"
}
else {
    $envLines += "数据库种子（回归前）：未取到（未检测到 docker，或容器 campushub-mysql 未运行）"
}

Save-Report 'environment' $envLines

$results = [ordered]@{}

# ---------- 1. 单元测试 ----------
if ($SkipUnit) {
    Write-Host "[1/3] 单元测试：已跳过（-SkipUnit）" -ForegroundColor Yellow
    $results['单元测试'] = 'SKIPPED'
}
else {
    Write-Host "[1/3] 单元测试：mvn -B -f backend/pom.xml test" -ForegroundColor Cyan
    # 本脚本把控制台输出设为 UTF-8，Maven JVM 默认按系统代码页输出，中文测试类名会变乱码，这里让它也走 UTF-8
    $previousMavenOpts = $env:MAVEN_OPTS
    $env:MAVEN_OPTS = (@('-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', $previousMavenOpts) |
        Where-Object { $_ }) -join ' '
    try {
        $unitOut  = & mvn -B -f (Join-Path $repoRoot 'backend\pom.xml') test 2>&1
        $unitExit = $LASTEXITCODE
    }
    finally {
        $env:MAVEN_OPTS = $previousMavenOpts
    }
    Save-Report 'unit' $unitOut
    $summary = $unitOut | Select-String -Pattern '^\[INFO\] Tests run:.*Failures' | Select-Object -Last 1
    $results['单元测试'] = if ($unitExit -eq 0) { "PASS  $($summary -replace '^\[INFO\]\s*','')" } else { "FAIL  exit=$unitExit" }
}

# ---------- 2. 集成测试 ----------
Write-Host "[2/3] 集成测试：tests/api-e2e.ps1" -ForegroundColor Cyan
$psExe    = if (Get-Command pwsh -ErrorAction SilentlyContinue) { 'pwsh' } else { 'powershell' }
$apiOut   = & $psExe -NoProfile -File (Join-Path $PSScriptRoot 'api-e2e.ps1') 2>&1
$apiExit  = $LASTEXITCODE
Save-Report 'api-e2e' $apiOut
$apiLine  = $apiOut | Select-String -Pattern '^通过 ' | Select-Object -Last 1
$results['集成测试'] = if ($apiExit -eq 0) { "PASS  $apiLine" } else { "FAIL  exit=$apiExit  $apiLine" }

# ---------- 3. 系统测试 ----------
Write-Host "[3/3] 系统测试：Playwright（frontend/e2e）" -ForegroundColor Cyan
Push-Location (Join-Path $repoRoot 'frontend')
try {
    $uiOut  = & npm run test:ui 2>&1
    $uiExit = $LASTEXITCODE
}
finally {
    Pop-Location
}
Save-Report 'playwright' $uiOut
$uiLine = $uiOut | Select-String -Pattern 'passed|failed' | Select-Object -Last 1
$results['系统测试'] = if ($uiExit -eq 0) { "PASS  $($uiLine -replace '\x1b\[[0-9;]*m','')" } else { "FAIL  exit=$uiExit  $uiLine" }

# ---------- 汇总 ----------
Write-Host ""
Write-Host "==== 回归结果（留档目录：$ReportDir）====" -ForegroundColor Green
foreach ($key in $results.Keys) {
    $color = if ("$($results[$key])" -like 'PASS*') { 'Green' } elseif ("$($results[$key])" -like 'SKIPPED*') { 'Yellow' } else { 'Red' }
    Write-Host ("  {0,-6} {1}" -f $key, $results[$key]) -ForegroundColor $color
}

$failed = @($results.Values | Where-Object { "$_" -like 'FAIL*' })
if ($failed.Count -gt 0) { exit 1 }
exit 0
