# CampusHub 接口 E2E 回归（覆盖接口类 AC 的关键断言）
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)

$base = 'http://localhost:8080/api'
$pass = 0
$fail = 0

function Check($name, $condition) {
    if ($condition) {
        $script:pass++
        Write-Host "[PASS] $name" -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host "[FAIL] $name" -ForegroundColor Red
    }
}

function PostJson($url, $body, $token) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Method Post -ContentType 'application/json; charset=utf-8' `
        -Body ([Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json))) -Headers $headers
}

function GetApi($url, $token) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Headers $headers
}

$suffix = (Get-Date).ToString('MMddHHmmss')
$studentNo = "9$suffix"

# 1. 匿名接口
$categories = GetApi "$base/categories"
Check '分类接口返回 6 个分类' ($categories.code -eq 0 -and $categories.data.Count -eq 6)
$items = GetApi "$base/items?pageSize=5"
Check '集市列表分页正常' ($items.code -eq 0 -and $items.data.list.Count -le 5 -and $items.data.total -gt 0)
$stats = GetApi "$base/system/stats"
Check '平台统计四项指标' ($stats.code -eq 0 -and $stats.data.onSaleCount -gt 0)

# 2. 注册与登录
$register = PostJson "$base/auth/register" @{
    studentNo = $studentNo; password = '123456'; nickname = 'E2E测试'; phone = '13800009999'
}
Check '注册成功并返回 JWT' ($register.code -eq 0 -and $register.data.token.Length -gt 20)
$token = $register.data.token
$me = GetApi "$base/auth/me" $token
Check 'JWT 可访问 /auth/me' ($me.code -eq 0 -and $me.data.studentNo -eq $studentNo)
$duplicate = PostJson "$base/auth/register" @{ studentNo = $studentNo; password = '123456'; nickname = '重复'; phone = '13800009998' }
Check '重复学号注册返回 4005' ($duplicate.code -eq 4005)
$badLogin = PostJson "$base/auth/login" @{ studentNo = $studentNo; password = 'wrong' }
Check '错误密码返回 4002' ($badLogin.code -eq 4002)

# 3. 商品详情与留言
$detail = GetApi "$base/items/1"
Check '商品详情包含卖家与图片' ($detail.code -eq 0 -and $detail.data.images.Count -gt 0 -and $detail.data.seller.phone)
$message = PostJson "$base/items/1/messages" @{ content = 'E2E 留言测试'; parentId = $null } $token
Check '登录用户可发表留言' ($message.code -eq 0)
$messageList = GetApi "$base/items/1/messages"
Check '留言列表可匿名查看' ($messageList.code -eq 0 -and $messageList.data.Count -gt 0)

# 4. 砍价
$bargain = PostJson "$base/bargains" @{ itemId = 1; offerPrice = 1; message = 'E2E 出价' } $token
Check '提交砍价成功' ($bargain.code -eq 0 -and $bargain.data.status -eq 'PENDING')
$myBargains = GetApi "$base/bargains?view=sent" $token
Check '砍价发送列表可见' ($myBargains.code -eq 0 -and $myBargains.data.list.Count -gt 0)

# 5. 预约
$reservation = PostJson "$base/reservations" @{ itemId = 1; expectTime = '2026-12-01T15:00:00'; remark = 'E2E 预约' } $token
Check '提交预约成功' ($reservation.code -eq 0 -and $reservation.data.status -eq 'PENDING')

# 6. 举报与权限
$report = PostJson "$base/reports" @{ itemId = 1; reason = 'FAKE'; detail = 'E2E 举报' } $token
Check '提交举报成功' ($report.code -eq 0)
$adminDenied = GetApi "$base/admin/users" $token
Check '普通用户访问管理接口返回 4003' ($adminDenied.code -eq 4003)

# 7. 管理员
$adminLogin = PostJson "$base/auth/login" @{ studentNo = 'admin'; password = '123456' }
Check '管理员登录成功' ($adminLogin.code -eq 0 -and $adminLogin.data.user.role -eq 'ADMIN')
$adminToken = $adminLogin.data.token
$adminUsers = GetApi "$base/admin/users" $adminToken
Check '管理员可查看用户列表' ($adminUsers.code -eq 0 -and $adminUsers.data.total -gt 0)
$adminReports = GetApi "$base/admin/reports" $adminToken
Check '管理员可查看举报列表' ($adminReports.code -eq 0 -and $adminReports.data.list.Count -gt 0)

# 8. 未登录 401
try {
    Invoke-RestMethod -Uri "$base/items/mine" -Method Get -ErrorAction Stop | Out-Null
    Check '未登录访问我的发布返回 401' $false
} catch {
    Check '未登录访问我的发布返回 401' ($_.Exception.Response.StatusCode.value__ -eq 401)
}

Write-Host ""
Write-Host "通过 $pass 项，失败 $fail 项" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 }
