# CampusHub 代码质量分析（SonarQube 自托管）
# 流程：拉起 SonarQube 容器 → 初始化管理员密码 → 生成令牌 → 跑测试并采集覆盖率 → 上传分析 → 抓取指标留档
# 用法：pwsh -File tests\run-sonar.ps1
param(
    [string]$SonarUrl      = 'http://localhost:9000',
    [string]$AdminUser     = 'admin',
    [string]$AdminPassword = 'Campushub-Sonar-2026',
    [string]$ProjectKey    = 'campushub-backend',
    [string]$ReportDir     = (Join-Path $PSScriptRoot 'reports\raw')
)

# 与 run-regression.ps1 同理：要驱动 docker / mvn 这类会往 stderr 写提示的命令，统一看退出码
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$stamp    = Get-Date -Format 'yyyy-MM-dd'
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

function Get-BasicAuth {
    param([string]$User, [string]$Password)
    @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$User`:$Password")) }
}

function Test-SonarUp {
    try {
        $r = Invoke-RestMethod "$SonarUrl/api/system/status" -TimeoutSec 8
        return ($r.status -eq 'UP')
    }
    catch {
        return $false
    }
}

# ---------- 1. 确保 SonarQube 在运行 ----------
if (-not (Test-SonarUp)) {
    Write-Host "[1/5] 启动 SonarQube 容器 ..." -ForegroundColor Cyan
    & docker compose -f (Join-Path $repoRoot 'docker-compose.sonar.yml') up -d 2>&1 | Out-Null

    $up = $false
    for ($i = 1; $i -le 60; $i++) {
        Start-Sleep -Seconds 10
        if (Test-SonarUp) { Write-Host "      已就绪（约 $($i * 10) 秒）"; $up = $true; break }
    }
    if (-not $up) {
        Write-Host "SonarQube 未在预期时间内就绪，请查看 docker logs campushub-sonarqube" -ForegroundColor Red
        exit 2
    }
}
else {
    Write-Host "[1/5] SonarQube 已在运行" -ForegroundColor Cyan
}

# ---------- 2. 管理员凭据：默认密码未改过则先改掉 ----------
Write-Host "[2/5] 校验管理员凭据 ..." -ForegroundColor Cyan
$valid = (Invoke-RestMethod "$SonarUrl/api/authentication/validate" -Headers (Get-BasicAuth $AdminUser $AdminPassword) -TimeoutSec 10).valid
if (-not $valid) {
    try {
        Invoke-RestMethod "$SonarUrl/api/users/change_password?login=$AdminUser&previousPassword=admin&password=$AdminPassword" `
            -Method Post -Headers (Get-BasicAuth $AdminUser 'admin') -TimeoutSec 15 | Out-Null
        Write-Host "      已将默认密码改为团队约定的分析密码"
    }
    catch {
        Write-Host "      无法用默认密码登录，请确认 $AdminUser 的密码是否为约定值" -ForegroundColor Red
        exit 2
    }
    $valid = (Invoke-RestMethod "$SonarUrl/api/authentication/validate" -Headers (Get-BasicAuth $AdminUser $AdminPassword) -TimeoutSec 10).valid
}
if (-not $valid) { Write-Host "管理员凭据不可用" -ForegroundColor Red; exit 2 }

# ---------- 3. 生成一次性分析令牌 ----------
Write-Host "[3/5] 生成分析令牌 ..." -ForegroundColor Cyan
$tokenName = "campushub-$stamp-$(Get-Date -Format HHmmss)"
$token = (Invoke-RestMethod "$SonarUrl/api/user_tokens/generate?name=$tokenName" -Method Post `
    -Headers (Get-BasicAuth $AdminUser $AdminPassword) -TimeoutSec 20).token
if (-not $token) { Write-Host "令牌生成失败" -ForegroundColor Red; exit 2 }
Write-Host "      令牌已生成（不写入任何文件）"

# ---------- 4. 跑测试 + 采集覆盖率 + 上传分析 ----------
Write-Host "[4/5] mvn verify sonar:sonar（含 92 项单测与 JaCoCo 覆盖率采集）..." -ForegroundColor Cyan
$scanOut  = & mvn -B -f (Join-Path $repoRoot 'backend\pom.xml') verify sonar:sonar `
    "-Dsonar.token=$token" "-Dsonar.host.url=$SonarUrl" 2>&1
$scanExit = $LASTEXITCODE
$scanOut | ForEach-Object { $_ -replace [regex]::Escape($token), '***' } |
    Set-Content -LiteralPath (Join-Path $ReportDir "$stamp-sonarqube-scan.txt") -Encoding utf8

if ($scanExit -ne 0) {
    Write-Host "分析失败，日志：$ReportDir\$stamp-sonarqube-scan.txt" -ForegroundColor Red
    exit 1
}

# ---------- 5. 抓取指标与质量门禁 ----------
Write-Host "[5/5] 抓取质量指标 ..." -ForegroundColor Cyan
Start-Sleep -Seconds 15
$auth   = @{ Authorization = "Bearer $token" }
$metric = 'bugs,vulnerabilities,security_hotspots,code_smells,coverage,line_coverage,branch_coverage,' +
          'duplicated_lines_density,ncloc,sqale_index,reliability_rating,security_rating,sqale_rating,' +
          'complexity,cognitive_complexity'
$measures = Invoke-RestMethod "$SonarUrl/api/measures/component?component=$ProjectKey&metricKeys=$metric" -Headers $auth -TimeoutSec 20
$gate     = Invoke-RestMethod "$SonarUrl/api/qualitygates/project_status?projectKey=$ProjectKey" -Headers $auth -TimeoutSec 20
$measures | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $ReportDir "$stamp-sonarqube-metrics.json") -Encoding utf8
$gate     | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $ReportDir "$stamp-sonarqube-qualitygate.json") -Encoding utf8

Write-Host ""
Write-Host "==== 代码质量结果（$SonarUrl/dashboard?id=$ProjectKey）====" -ForegroundColor Green
Write-Host ("  质量门禁：{0}" -f $gate.projectStatus.status)
foreach ($m in $measures.component.measures) {
    Write-Host ("  {0,-30} {1}" -f $m.metric, $m.value)
}
Write-Host "  留档：$ReportDir\$stamp-sonarqube-*.json / -scan.txt" -ForegroundColor DarkGray
