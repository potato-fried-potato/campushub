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
    $resp = Invoke-WebRequest -Uri $url -Method Post -ContentType 'application/json; charset=utf-8' `
        -Body ([Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json))) -Headers $headers -UseBasicParsing
    return ReadJson $resp
}

function GetApi($url, $token) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    return ReadJson (Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing)
}

function PatchJson($url, $body, $token) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    $resp = Invoke-WebRequest -Uri $url -Method Patch -ContentType 'application/json; charset=utf-8' `
        -Body ([Text.Encoding]::UTF8.GetBytes(($body | ConvertTo-Json))) -Headers $headers -UseBasicParsing
    return ReadJson $resp
}

# Windows PowerShell 5.1 默认按 Latin-1 解析 JSON 响应，中文会变乱码，这里统一按 UTF-8 解码
function ReadJson($response) {
    return ConvertFrom-Json ([Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray()))
}

# 1x1 透明 PNG，仅用于让发布接口拿到合法 Data URI
$tinyPng = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII='

# 每个用例发布一件全新商品，保证脚本可重复执行
function NewTestItem($token, $title, $price) {
    $res = PostJson "$base/items" @{
        title = $title; description = 'E2E 交易闭环用例'; price = $price; originalPrice = $price + 20
        conditionLevel = 'NEARLY_NEW'; campus = 'DONGFENG'; categoryId = 1; images = @($tinyPng)
    } $token
    return $res.data.id
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
Check '商品详情包含卖家名片与图片' ($detail.code -eq 0 -and $detail.data.images.Count -gt 0 -and $detail.data.seller.nickname -and $detail.data.seller.studentNo)
Check '商品详情不公开卖家手机号' ($null -eq $detail.data.seller.phone)
$message = PostJson "$base/items/1/messages" @{ content = 'E2E 留言测试'; parentId = $null } $token
Check '登录用户可发表留言' ($message.code -eq 0)
$messageList = GetApi "$base/items/1/messages"
Check '留言列表可匿名查看' ($messageList.code -eq 0 -and $messageList.data.Count -gt 0)

# 4. 砍价
$lowBall = PostJson "$base/bargains" @{ itemId = 4; offerPrice = 1; message = 'E2E 低价出价' } $token
Check '低于挂牌价 8 折的出价被拒' ($lowBall.code -eq 4001)
$bargain = PostJson "$base/bargains" @{ itemId = 1; offerPrice = 20; message = 'E2E 出价' } $token
Check '按挂牌价 8 折下限出价成功' ($bargain.code -eq 0 -and $bargain.data.status -eq 'PENDING')
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
$adminBrowse = GetApi "$base/items?pageSize=3" $adminToken
Check '管理员可只读浏览集市' ($adminBrowse.code -eq 0)
$adminBargain = PostJson "$base/bargains" @{ itemId = 1; offerPrice = 22; message = 'E2E 管理员出价' } $adminToken
Check '管理员账号不参与用户交易（出价被拒）' ($adminBargain.code -eq 4003)
$adminReservation = PostJson "$base/reservations" @{ itemId = 1; remark = 'E2E 管理员预约' } $adminToken
Check '管理员账号不参与用户交易（预约被拒）' ($adminReservation.code -eq 4003)
$adminMessage = PostJson "$base/items/1/messages" @{ content = 'E2E 管理员留言' } $adminToken
Check '管理员账号不参与用户交易（留言被拒）' ($adminMessage.code -eq 4003)

# 8. 未登录 401
try {
    Invoke-RestMethod -Uri "$base/items/mine" -Method Get -ErrorAction Stop | Out-Null
    Check '未登录访问我的发布返回 401' $false
} catch {
    Check '未登录访问我的发布返回 401' ($_.Exception.Response.StatusCode.value__ -eq 401)
}

# 9. 交易闭环：立即购买 -> 确认成交 -> 互评
$buyerLogin = PostJson "$base/auth/login" @{ studentNo = '542412310217'; password = '123456' }
Check '种子买家账号可登录' ($buyerLogin.code -eq 0)
$buyerToken = $buyerLogin.data.token

$itemA = NewTestItem $token "E2E立即购买$suffix" 66
Check '发布立即购买测试商品' ($itemA -gt 0)
$detailBefore = GetApi "$base/items/$itemA" $buyerToken
Check '买家详情显示可立即购买' ($detailBefore.data.canDirectBuy -eq $true -and $detailBefore.data.reserved -eq $false)

$direct = PostJson "$base/deals/direct" @{ itemId = $itemA } $buyerToken
Check '立即购买生成待成交交易单' ($direct.code -eq 0 -and $direct.data.status -eq 'PENDING' -and $direct.data.source -eq 'DIRECT')
$dealId = $direct.data.id

$detailLocked = GetApi "$base/items/$itemA" $buyerToken
Check '待成交交易单锁定商品' ($detailLocked.data.reserved -eq $true -and $detailLocked.data.dealStatus -eq 'PENDING' -and $detailLocked.data.canDirectBuy -eq $false)
$dupDirect = PostJson "$base/deals/direct" @{ itemId = $itemA } $buyerToken
Check '锁定期间重复下单返回 4005' ($dupDirect.code -eq 4005)
$lockedBargain = PostJson "$base/bargains" @{ itemId = $itemA; offerPrice = 60; message = 'E2E 锁定出价' } $buyerToken
Check '锁定期间出价返回 4005' ($lockedBargain.code -eq 4005)
$lockedReservation = PostJson "$base/reservations" @{ itemId = $itemA; expectTime = '2026-12-02T15:00:00'; remark = 'E2E 锁定预约' } $buyerToken
Check '锁定期间预约返回 4005' ($lockedReservation.code -eq 4005)

$sellerDeals = GetApi "$base/deals?view=received" $token
Check '卖家交易单列表含待成交单' ($sellerDeals.code -eq 0 -and $sellerDeals.data.pendingCount -ge 1)
$foreignComplete = PostJson "$base/deals/$dealId/complete" @{} $adminToken
Check '非交易双方不能确认成交' ($foreignComplete.code -eq 4003)

$complete = PostJson "$base/deals/$dealId/complete" @{} $token
Check '卖家确认成交成功' ($complete.code -eq 0)
$detailDone = GetApi "$base/items/$itemA" $buyerToken
Check '成交后商品已售且开放评价' ($detailDone.data.status -eq 'SOLD' -and $detailDone.data.dealStatus -eq 'COMPLETED' -and $detailDone.data.canReview -eq $true)

$reviewA = PostJson "$base/reviews" @{ itemId = $itemA; rating = 5; content = 'E2E 立即购买评价' } $buyerToken
Check '买家成交后可提交评价' ($reviewA.code -eq 0)
$detailReviewed = GetApi "$base/items/$itemA" $buyerToken
Check '评价完成后评价入口关闭' ($detailReviewed.data.canReview -eq $false)
$reviewDup = PostJson "$base/reviews" @{ itemId = $itemA; rating = 4; content = 'E2E 重复评价' } $buyerToken
Check '重复评价返回 4005' ($reviewDup.code -eq 4005)

# 交易单私密会话：仅买卖双方可见，与公开的商品问答完全分离
$chatSend = PostJson "$base/deals/$dealId/messages" @{ content = 'E2E private chat ping' } $buyerToken
Check '买家可在交易单私密会话发言' ($chatSend.code -eq 0)
$chat = GetApi "$base/deals/$dealId/messages" $token
$chatOk = ($chat.code -eq 0) -and ($chat.data.list.Count -eq 1) `
    -and ($chat.data.list[0].content -eq 'E2E private chat ping') -and ($chat.data.list[0].mine -eq $false) `
    -and ($chat.data.peerNickname -eq '宋杰')
if (-not $chatOk) {
    Write-Host ("  会话实际返回：code=" + $chat.code + " count=" + $chat.data.list.Count + " peer=" + $chat.data.peerNickname) -ForegroundColor DarkYellow
}
Check '卖家可读取私密会话内容' $chatOk
$chatForeign = GetApi "$base/deals/$dealId/messages" $adminToken
Check '非交易方读取私密会话返回 4003' ($chatForeign.code -eq 4003)
$chatForeignSend = PostJson "$base/deals/$dealId/messages" @{ content = 'E2E 越权私聊' } $adminToken
Check '非交易方发言返回 4003' ($chatForeignSend.code -eq 4003)
$chatEmpty = PostJson "$base/deals/$dealId/messages" @{ content = '   ' } $buyerToken
Check '空消息返回 4001' ($chatEmpty.code -eq 4001)
$publicQa = GetApi "$base/items/$itemA/messages"
$qaText = (@($publicQa.data) | ForEach-Object { $_.content; ($_.replies | ForEach-Object { $_.content }) }) -join "`n"
Check '私密会话不出现在公开商品问答' ($qaText -notlike '*E2E private chat ping*')

# 10. 砍价成交：接受出价生成交易单，可再取消解锁
$itemB = NewTestItem $token "E2E砍价成交$suffix" 88
$bargainB = PostJson "$base/bargains" @{ itemId = $itemB; offerPrice = 80; message = 'E2E 砍价成交' } $buyerToken
Check '新商品可提交砍价' ($bargainB.code -eq 0)
$acceptB = PostJson "$base/bargains/$($bargainB.data.id)/accept" @{} $token
Check '接受出价生成交易单' ($acceptB.code -eq 0)
$dealB = (GetApi "$base/deals?view=sent" $buyerToken).data.list | Where-Object { $_.itemId -eq $itemB } | Select-Object -First 1
Check '砍价交易单来源与价格正确' ($dealB.source -eq 'BARGAIN' -and [decimal]$dealB.dealPrice -eq 80)
$detailB = GetApi "$base/items/$itemB" $buyerToken
Check '接受出价后仍待在售待成交' ($detailB.data.status -eq 'ON_SALE' -and $detailB.data.dealStatus -eq 'PENDING')
$cancelB = PostJson "$base/deals/$($dealB.id)/cancel" @{} $token
Check '取消交易单成功' ($cancelB.code -eq 0)
$detailBCancelled = GetApi "$base/items/$itemB" $token
Check '取消后商品解锁并回到在售' ($detailBCancelled.data.status -eq 'ON_SALE' -and $detailBCancelled.data.reserved -eq $false -and $detailBCancelled.data.dealStatus -ne 'PENDING')
$rebuyB = PostJson "$base/deals/direct" @{ itemId = $itemB } $buyerToken
Check '取消后可重新下单' ($rebuyB.code -eq 0)
PostJson "$base/deals/$($rebuyB.data.id)/cancel" @{} $buyerToken | Out-Null
$chatCancelled = PostJson "$base/deals/$($dealB.id)/messages" @{ content = 'E2E 已取消单发言' } $token
Check '已取消交易单只读不可发言' ($chatCancelled.code -eq 4005)

# 11. 预约成交：确认预约生成交易单
$itemC = NewTestItem $token "E2E预约成交$suffix" 120
$reservationC = PostJson "$base/reservations" @{ itemId = $itemC; expectTime = '2026-12-03T15:00:00'; remark = 'E2E 预约成交' } $buyerToken
Check '新商品可提交预约' ($reservationC.code -eq 0)
$confirmC = PostJson "$base/reservations/$($reservationC.data.id)/confirm" @{} $token
Check '确认预约生成交易单' ($confirmC.code -eq 0)
$resC = (GetApi "$base/reservations?view=sent" $buyerToken).data.list | Where-Object { $_.id -eq $reservationC.data.id } | Select-Object -First 1
Check '预约状态置为已确认' ($resC.status -eq 'CONFIRMED')
$dealC = (GetApi "$base/deals?view=received" $token).data.list | Where-Object { $_.itemId -eq $itemC } | Select-Object -First 1
Check '预约交易单来源正确' ($dealC.source -eq 'RESERVATION')
$completeC = PostJson "$base/deals/$($dealC.id)/complete" @{} $token
Check '预约交易单可确认成交' ($completeC.code -eq 0)

# 12. 线下已售：无交易单、不可评价
$itemD = NewTestItem $token "E2E线下已售$suffix" 30
$offline = PatchJson "$base/items/$itemD/status" @{ status = 'SOLD' } $token
Check '卖家可标记线下已售' ($offline.code -eq 0)
$reviewD = PostJson "$base/reviews" @{ itemId = $itemD; rating = 5; content = 'E2E 线下已售评价' } $buyerToken
Check '线下已售商品不可评价' ($reviewD.code -eq 4003)
$detailD = GetApi "$base/items/$itemD" $token
Check '线下已售不产生交易单' ($detailD.data.status -eq 'SOLD' -and $null -eq $detailD.data.dealStatus)

# 13. 举报处置闭环：管理员下架被举报商品（AC-11.3 / AC-12.4）
$itemE = NewTestItem $token "E2E举报处置$suffix" 50
$reportE = PostJson "$base/reports" @{ itemId = $itemE; reason = 'FAKE'; detail = 'E2E 举报处置闭环' } $buyerToken
Check '可对商品提交举报（处置前置）' ($reportE.code -eq 0)
$handleE = PatchJson "$base/admin/reports/$($reportE.data.id)" @{ action = 'OFF_SHELF' } $adminToken
Check '管理员处置举报成功' ($handleE.code -eq 0)
$detailE = GetApi "$base/items/$itemE" $token
Check '举报处置后商品被强制下架' ($detailE.data.status -eq 'OFF_SHELF')

Write-Host ""
Write-Host "通过 $pass 项，失败 $fail 项" -ForegroundColor Cyan
if ($fail -gt 0) { exit 1 }
