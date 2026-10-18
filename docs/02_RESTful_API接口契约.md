# 智校互联 (CampusHub) 校园二手交易协同平台
## 02. 前后端 RESTful API 接口契约说明书

> **课程班级**：软件工程 2402 · 第 11 组  
> **制定人员**：边皓东（组长）  
> **生效版本**：v1.1.0（与 01 需求基线 v1.7.0、05 分工 v1.6.0 配套，新增交易单接口 10.7 ~ 10.12）  
> **维护规则**：契约先行——任何接口变更必须先更新本文档，再修改代码（见 `docs/04`）。

---

### 一、通用约定

1. **Base URL**：所有接口以 `/api` 为前缀；生产环境由 Spring Boot 同源托管前端，无跨域问题；开发环境 Vite 将 `/api` 代理至 `http://localhost:8080`；
2. **数据格式**：请求与响应均为 `application/json; charset=UTF-8`；文件上传使用 `multipart/form-data`；
3. **统一响应体**：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

4. **分页结构**：`data` 统一为 `{ "list": [], "total": 0, "pageNum": 1, "pageSize": 10 }`；
5. **HTTP 状态语义**：仅「未登录 / Token 失效」返回 HTTP 401；其余业务结果一律 HTTP 200，通过 `code` 区分成败与错误原因；
6. **认证方式**：除匿名接口外，请求头必须携带 `Authorization: Bearer <JWT>`；JWT 使用 HS256 签名，有效期 7 天，载荷含 `sub`（用户 id）、`studentNo`、`nickname`、`role`。

---

### 二、业务码表

| code | 含义 | 典型场景 |
| :---: | :--- | :--- |
| 0 | 成功 | — |
| 4001 | 参数校验失败 | 标题为空、价格非法、超出长度限制 |
| 4002 | 学号或密码错误 | 登录失败 |
| 4003 | 权限不足 / 越权 | 操作他人商品、非管理员访问后台接口、管理员调用用户侧写接口、非交易双方读写交易单 |
| 4004 | 资源不存在 | 商品 / 留言 / 预约 / 出价 id 无效 |
| 4005 | 业务冲突 | 重复出价、重复预约、重复评价、重复举报、状态不允许 |
| 4006 | 文件不合法 | 图片魔数校验失败、超过 5MB |
| 4007 | 账号已被禁用 | 被禁用账号尝试登录 |
| 5000 | 服务器内部错误 | 未捕获异常 |

错误响应示例：

```json
{ "code": 4002, "message": "学号或密码错误", "data": null }
```

未登录响应（HTTP 401）：

```json
{ "code": 401, "message": "未登录或登录已过期", "data": null }
```

---

### 三、枚举字典

| 枚举 | 取值 | 说明 |
| :--- | :--- | :--- |
| 商品状态 `item.status` | `ON_SALE` / `OFF_SHELF` / `SOLD` | 在售 / 下架 / 已售 |
| 成色 `conditionLevel` | `NEW` / `NEARLY_NEW` / `GOOD` | 全新 / 9 成新 / 8 成新 |
| 校区 `campus` | `DONGFENG` / `KEXUE` | 东风校区 / 科学校区 |
| 排序 `sort` | `latest` / `priceAsc` / `priceDesc` / `views` | 最新发布 / 价格最低 / 价格最高 / 最多浏览 |
| 砍价状态 `bargain.status` | `PENDING` / `ACCEPTED` / `REJECTED` / `CANCELLED` | 待处理 / 已接受 / 已拒绝 / 已取消 |
| 预约状态 `reservation.status` | `PENDING` / `CONFIRMED` / `REJECTED` / `CANCELLED` / `CLOSED` | 待确认 / 已确认 / 已拒绝 / 已取消 / 已关闭 |
| 举报原因 `report.reason` | `FAKE` / `ILLEGAL` / `SCAM` / `OTHER` | 虚假信息 / 违禁物品 / 诈骗风险 / 其他 |
| 举报状态 `report.status` | `PENDING` / `OFF_SHELF` / `REJECTED` | 待处理 / 已下架 / 已驳回 |
| 交易单来源 `deal.source` | `DIRECT` / `BARGAIN` / `RESERVATION` | 立即购买 / 砍价成交 / 预约成交 |
| 交易单状态 `deal.status` | `PENDING` / `COMPLETED` / `CANCELLED` | 待成交（锁定商品）/ 已完成 / 已取消 |
| 用户角色 `user.role` | `USER` / `ADMIN` | 学生用户 / 管理员 |
| 用户状态 `user.status` | `ACTIVE` / `DISABLED` | 正常 / 已禁用 |

---

### 四、权限矩阵

| 接口组 | 匿名 | 登录用户 | 管理员 |
| :--- | :---: | :---: | :---: |
| 注册 / 登录 | ✓ | — | — |
| 分类 / 统计 / 商品浏览 / 留言查看 / 评价查看 | ✓ | ✓ | ✓ |
| 商品发布 / 图片上传 / 我的发布 / 状态流转 | — | ✓（仅本人商品） | ✗（管理员不参与用户交易，强制下架走 `/api/admin/**`） |
| 留言发表与回复 | — | ✓（按会话角色校验） | — |
| 砍价提交与处理 / 预约提交与处理 | — | ✓（买家提交、卖家处理） | — |
| 评价 / 举报 | — | ✓（成交双方 / 非本人商品） | — |
| `/api/admin/**` 管理后台 | — | — | ✓ |

> 所有标注「仅本人商品」「按会话角色」的接口，服务端必须做越权校验，违规返回业务码 `4003`（AC-1.3、AC-5.2、AC-12.1）。
>
> **管理员隔离**：管理端与用户端分开——管理员账号只允许「认证接口 + `/api/admin/**` + 只读浏览」，其余用户侧写操作（发布 / 出价 / 预约 / 留言 / 评价 / 举报 / 交易单操作等）统一由 `AuthInterceptor` 拦截，返回业务码 `4003`、提示「管理员账号仅用于后台管理，不参与用户交易活动」（AC-12.1）。

---

### 五、认证模块（M1 用户模块）

#### 5.1 注册

`POST /api/auth/register`（匿名）

请求体：

```json
{
  "studentNo": "542412320XXX",
  "password": "123456",
  "nickname": "新同学",
  "phone": "13800000000"
}
```

校验规则：`studentNo` 6~20 位字母/数字且唯一；`password` 6~20 位；`nickname` 1~16 字；`phone` 11 位手机号。

响应 `data`（注册成功自动登录，并生成内联 SVG 头像，AC-1.4）：

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": 15,
    "studentNo": "542412320XXX",
    "nickname": "新同学",
    "phone": "13800000000",
    "role": "USER",
    "avatar": "data:image/svg+xml;base64,..."
  }
}
```

错误：`4001` 参数校验失败；`4005` 学号已被注册。

#### 5.2 登录

`POST /api/auth/login`（匿名）

请求体：`{ "studentNo": "542412320201", "password": "123456" }`

响应 `data`：同 5.1（`token` + `user`，密码使用 BCrypt 校验，AC-1.2）。

错误：`4002` 学号或密码错误（AC-1.1）；`4007` 账号已被禁用（AC-12.2）。

#### 5.3 当前用户信息

`GET /api/auth/me`（登录）

响应 `data`：当前用户对象（字段同 5.1 的 `user`）。

#### 5.4 退出

`POST /api/auth/logout`（登录）

响应 `data: null`。说明：JWT 无状态，服务端不做会话销毁，前端负责清除本地 Token。

---

### 六、分类与系统（M4 平台门户模块）

#### 6.1 分类列表

`GET /api/categories`（匿名）

响应 `data`（含各分类在售数量，AC-2.2）：

```json
[
  { "id": 1, "name": "教材图书", "icon": "book", "onSaleCount": 8 },
  { "id": 2, "name": "数码 3C", "icon": "device", "onSaleCount": 6 }
]
```

#### 6.2 平台统计

`GET /api/system/stats`（匿名）

响应 `data`（AC-8.1、AC-12.5）：

```json
{
  "onSaleCount": 21,
  "soldCount": 11,
  "certifiedUserCount": 13,
  "totalViewCount": 1234
}
```

说明：认证用户数 = 状态为 `ACTIVE` 的学生用户数（不含管理员）；累计浏览量 = 全部商品浏览量之和。

---

### 七、商品模块（M2 商品模块）

#### 7.1 集市列表（检索 / 筛选 / 排序）

`GET /api/items`（匿名可访问）

| 参数 | 类型 | 必填 | 说明 |
| :--- | :--- | :---: | :--- |
| keyword | string | 否 | 标题与描述模糊匹配 |
| categoryId | long | 否 | 分类筛选 |
| conditionLevel | string | 否 | 成色筛选 |
| campus | string | 否 | 校区筛选 |
| status | string | 否 | 默认 `ON_SALE`，可切换 `SOLD`（AC-2.1） |
| sort | string | 否 | 默认 `latest` |
| pageNum / pageSize | int | 否 | 默认 1 / 12 |

说明：**登录态访问时自动排除自己发布的商品**；匿名浏览不受影响（AC-2.5）。

响应 `data.list[]` 卡片字段：`id, title, coverImage, price, originalPrice, conditionLevel, campus, categoryId, categoryName, status, reserved, viewCount, createdAt`（`reserved` 表示该商品已被预约锁定，AC-10.3）。

#### 7.2 商品详情

`GET /api/items/{id}`（匿名）

说明：每次调用浏览量 +1（AC-3.4）。

响应 `data`：商品全字段 + 多图 + 卖家名片：

```json
{
  "id": 1,
  "title": "九成新数据结构教材",
  "description": "仅翻阅少量笔记，无破损",
  "price": 25.00,
  "originalPrice": 59.00,
  "bargainFloor": 20.00,
  "conditionLevel": "NEARLY_NEW",
  "campus": "DONGFENG",
  "categoryId": 1,
  "categoryName": "教材图书",
  "status": "ON_SALE",
  "reserved": false,
  "viewCount": 42,
  "images": ["data:image/jpeg;base64,..."],
  "seller": {
    "id": 1,
    "nickname": "边皓东",
    "studentNo": "542412320201",
    "avgRating": 4.8,
    "reviewCount": 12,
    "creditScore": 85
  },
  "createdAt": "2026-10-01T10:00:00",
  "isSeller": false,
  "isBuyer": false,
  "dealId": null,
  "dealStatus": null,
  "dealSource": null,
  "dealPrice": null,
  "canDirectBuy": true,
  "canReview": false,
  "canReport": true
}
```

> 说明：`isSeller` / `isBuyer` 表示当前登录用户是否为本商品的卖家或成交买家（匿名访问均为 `false`），`canReview` 为「商品已售且当前用户是交易参与方」，`canReport` 为「非本人商品且在售」。前端据此决定是否展示评价、举报入口（AC-11.2、AC-11.3）。
>
> **隐私约定**：详情接口**不返回卖家手机号**，联系方式一律走站内（AC-3.3）——未成交时使用公开的商品问答，成交后使用交易单私密会话；手机号仅管理员后台可见。

> 交易相关字段：`dealId`（当前生效或最近一张交易单 id）、`dealStatus`（`PENDING` / `COMPLETED` / `CANCELLED`）、`dealSource`（`DIRECT` / `BARGAIN` / `RESERVATION`）、`dealPrice`（仅交易单 `COMPLETED` 时返回实际成交价，否则为 `null`）、`reserved`（存在 `PENDING` 交易单时为 `true`）、`canDirectBuy`（登录用户且非卖家、商品在售且无 `PENDING` 交易单时为 `true`）。`canReview` 以交易单 `COMPLETED` 且当前用户为该交易单买卖一方为准；`bargainFloor` 为砍价下限（挂牌价 × 0.8，AC-6.1），前端据此展示可出价区间。

错误：`4004` 商品不存在。

#### 7.3 发布商品

`POST /api/items`（登录）

请求体：

```json
{
  "title": "九成新数据结构教材",
  "description": "仅翻阅少量笔记，无破损",
  "categoryId": 1,
  "price": 25.00,
  "originalPrice": 59.00,
  "conditionLevel": "NEARLY_NEW",
  "campus": "DONGFENG",
  "images": ["data:image/jpeg;base64,..."]
}
```

校验规则（AC-4.1）：`title` 2~40 字；`price` > 0 且 ≤ 99999；`images` 至少 1 张，单张 ≤ 5MB 且为允许的 Data URI 类型；`originalPrice` 可空，为空时前端不展示「立省」对比。

响应 `data`：`{ "id": 33 }`（发布成功后前端跳转该商品详情页，AC-4.5）。

错误：`4001`、`4006`。

#### 7.4 图片上传

`POST /api/files/upload`（登录，`multipart/form-data`）

表单字段：`file`（仅允许 JPG / PNG / WEBP / GIF，服务端校验文件头魔数，≤ 5MB，AC-4.2）。

响应 `data`：`{ "url": "data:image/png;base64,..." }`（AC-4.3，可将该 Data URI 放入发布请求的 `images` 数组）。

错误：`4006`；未登录返回 HTTP 401（AC-4.4）。

#### 7.5 我的发布

`GET /api/items/mine`（登录）

参数：`status = ON_SALE / OFF_SHELF / SOLD / ALL`（默认 `ALL`）；分页参数同 7.1。

响应 `data`：仅当前登录用户发布的商品（AC-5.1）。

#### 7.6 商品状态流转

`PATCH /api/items/{id}/status`（登录）

请求体：`{ "status": "OFF_SHELF" }`

允许流转（AC-5.2）：`ON_SALE → OFF_SHELF / SOLD`；`OFF_SHELF → ON_SALE`。

权限：仅商品发布者本人，越权返回 `4003`；**管理员不能通过该接口操作商品**（`AuthInterceptor` 拦截），违规商品强制下架走 `PATCH /api/admin/items/{id}/off-shelf`（AC-12.3）。

错误：`4003`、`4004`、`4005`（非法流转）。

---

### 八、互动模块：留言（M3 交易与互动模块）

#### 8.1 发表留言 / 回复

`POST /api/items/{itemId}/messages`（登录）

请求体：`{ "content": "这个还在吗？", "parentId": null }`

校验（AC-9.1、AC-9.2）：`content` 1~200 字；`parentId` 为空表示首条留言，非空表示回复（目标留言须属于同一商品）；无关用户回复返回 `4003`。

错误：`4001`、`4003`、`4004`；未登录返回 HTTP 401。

#### 8.2 商品留言列表

`GET /api/items/{itemId}/messages`（匿名）

响应 `data`：按时间倒序的留言话题列表，含双方昵称与时间（AC-9.3、AC-9.4）：

```json
[
  {
    "id": 1,
    "content": "这个还在吗？",
    "sender": { "id": 2, "nickname": "宋杰" },
    "createdAt": "2026-10-02T09:00:00",
    "replies": [
      { "id": 2, "content": "在的，可以东风校区面交", "sender": { "id": 1, "nickname": "边皓东" }, "createdAt": "2026-10-02T09:30:00" }
    ]
  }
]
```

#### 8.3 我的留言（双视角 + 未读提醒）

`GET /api/messages/mine`（登录）

参数：`view = received`（默认，我收到的）或 `sent`（我发出的）。

响应 `data`（AC-9.5）：

```json
{
  "totalUnread": 2,
  "list": [
    {
      "itemId": 1,
      "itemTitle": "九成新数据结构教材",
      "peerNickname": "宋杰",
      "lastContent": "这个还在吗？",
      "unreadCount": 1,
      "lastTime": "2026-10-02T09:00:00"
    }
  ]
}
```

#### 8.4 标记已读

`PATCH /api/messages/{id}/read`（登录）

权限：仅消息接收者，其它用户返回 `4003`。响应 `data: null`。

`PATCH /api/messages/item/{itemId}/read`（登录）

权限：仅该商品会话的接收者。一次性把当前用户在该商品会话下的未读留言全部置为已读，供交易中心「标记已读」使用。响应 `data: null`。

---

### 九、交易模块：砍价（M3 交易与互动模块）

#### 9.1 提交出价

`POST /api/bargains`（登录）

请求体：`{ "itemId": 1, "offerPrice": 20.00, "message": "20 出吗" }`

校验（AC-6.1、AC-6.2）：`offerPrice` 必须低于当前挂牌价，且**不得低于挂牌价的 80%（8 折）**，即 `挂牌价 × 0.8 ≤ offerPrice < 挂牌价`（下限按四舍五入保留两位小数，如挂牌价 25.00 → 下限 20.00）；不能对自己的商品出价；商品必须为 `ON_SALE`；同一买家对同一商品不能存在 `PENDING` 出价。商品详情 `GET /api/items/{id}` 返回 `bargainFloor` 字段供前端展示与预校验。

响应 `data`：`{ "id": 6, "status": "PENDING" }`。

错误：`4001`（出价不低于挂牌价或低于 8 折下限）、`4003`、`4004`、`4005`。

#### 9.2 砍价列表（双视角 + 角标）

`GET /api/bargains`（登录）

参数：`view = received`（默认）或 `sent`；`status` 可选过滤。

响应 `data`（AC-6.6）：

```json
{
  "pendingCount": 2,
  "list": [
    {
      "id": 6,
      "itemId": 1,
      "itemTitle": "九成新数据结构教材",
      "itemPrice": 25.00,
      "offerPrice": 20.00,
      "message": "20 出吗",
      "status": "PENDING",
      "buyerNickname": "宋杰",
      "createdAt": "2026-10-03T14:00:00",
      "handledAt": null
    }
  ]
}
```

#### 9.3 接受出价（成交联动）

`POST /api/bargains/{id}/accept`（卖家）

事务联动（AC-6.4）：出价置为 `ACCEPTED`；生成一张 `PENDING` 交易单（`source = BARGAIN`、`dealPrice = offerPrice`）；同商品其它 `PENDING` 出价自动置为 `REJECTED`；进行中的预约自动置为 `CLOSED`；商品锁定为「待成交」（不直接置 `SOLD`，需交易单 `COMPLETED`）。

错误：`4003`、`4004`、`4005`（已处理或商品非在售）。

#### 9.4 拒绝出价

`POST /api/bargains/{id}/reject`（卖家）

响应 `data: null`；错误同 9.3。

#### 9.5 取消出价

`POST /api/bargains/{id}/cancel`（买家）

仅 `PENDING` 状态可取消（AC-6.5，已处理记录不可重复操作）。

响应 `data: null`。

---

### 十、交易模块：交易单与意向预约（M3 交易与互动模块）

#### 10.1 提交预约

`POST /api/reservations`（登录）

请求体：`{ "itemId": 1, "expectTime": "2026-11-01T15:00:00", "remark": "周末东风校区当面交易" }`

校验（AC-10.1、AC-10.2、AC-10.4）：不能预约自己发布的商品；商品必须为 `ON_SALE` 且未被交易单锁定（不存在 `PENDING` 交易单）；同一买家对同一商品不能存在进行中（`PENDING` / `CONFIRMED`）预约。

响应 `data`：`{ "id": 1, "status": "PENDING" }`。

#### 10.2 预约列表（双视角）

`GET /api/reservations`（登录）

参数：`view = received`（默认）或 `sent`；`status` 可选过滤。

响应 `data`：结构与 9.2 类似（`pendingCount` + 列表），列表字段为 `id, itemId, itemTitle, expectTime, remark, status, buyerNickname, createdAt, handledAt`。

#### 10.3 确认预约（生成交易单并锁定商品）

`POST /api/reservations/{id}/confirm`（卖家）

事务联动（AC-10.3、AC-10.4）：预约置为 `CONFIRMED`；生成一张 `PENDING` 交易单（`source = RESERVATION`、`dealPrice = 挂牌价`）；商品进入「待成交」锁定，集市与详情页 `reserved = true`；锁定期间其他买家不可立即购买、不可出价、不可提交预约。

#### 10.4 拒绝预约

`POST /api/reservations/{id}/reject`（卖家）

联动（AC-10.4）：预约置为 `REJECTED`；商品自动解锁。

#### 10.5 取消预约

`POST /api/reservations/{id}/cancel`（买家）

联动：预约置为 `CANCELLED`；商品自动解锁。

#### 10.6 自动关闭说明

交易单 `COMPLETED`（商品置 `SOLD`）时，该商品所有进行中预约自动置为 `CLOSED`（AC-10.7）；卖家「线下已售」同样关闭进行中预约。

#### 10.7 立即购买（按挂牌价下单）

`POST /api/deals/direct`（登录）

请求体：`{ "itemId": 1 }`

校验（AC-10.1、AC-10.4）：不能购买自己发布的商品；商品必须为 `ON_SALE` 且不存在 `PENDING` 交易单。

事务联动：生成一张 `PENDING` 交易单（`source = DIRECT`、`dealPrice = 挂牌价`），商品锁定「待成交」；同商品其他 `PENDING` 出价置 `REJECTED`、进行中预约置 `CLOSED`。

响应 `data`：`{ "id": 3, "status": "PENDING", "source": "DIRECT", "dealPrice": 25.00 }`。

错误：`4001`、`4003`、`4004`、`4005`（商品不可购买或已被锁定）。

#### 10.8 我的交易单（双视角）

`GET /api/deals`（登录）

参数：`view = received`（默认，我作为卖家）或 `sent`（我作为买家）；`status` 可选过滤。

响应 `data`：`pendingCount`（待成交数）、`unreadMessageCount`（交易单私聊未读数，用于顶部待办角标）+ 列表，列表字段为 `id, itemId, itemTitle, itemCover, itemPrice, dealPrice, source, status, buyerNickname, sellerNickname, reviewed（当前用户是否已评价）, createdAt, completedAt, cancelledAt`。

#### 10.9 确认成交

`POST /api/deals/{id}/complete`（交易单买卖任一方）

事务联动（AC-10.6、AC-10.7）：交易单置为 `COMPLETED` 并记录 `completed_at`；商品置为 `SOLD` 并写入成交快照（`sold_buyer_id` / `sold_price` / `sold_at`）；同商品其他进行中预约置为 `CLOSED`。

错误：`4003`（非交易双方）、`4004`、`4005`（交易单非 `PENDING`）。

#### 10.10 取消交易单

`POST /api/deals/{id}/cancel`（交易单买卖任一方）

事务联动（AC-10.5）：交易单置为 `CANCELLED` 并记录 `cancelled_at`；商品回到 `ON_SALE` 解除锁定；来源为预约时该预约置为 `CLOSED`。

错误：`4003`、`4004`、`4005`。

#### 10.11 交易单私密会话（读取）

`GET /api/deals/{id}/messages`（仅该交易单的买家 / 卖家）

返回该交易单的私聊记录并同时把对方发来的消息置为已读（AC-10.8）。**该会话与公开的商品问答（9.1 / 9.2 `item_message`）是两套数据**：商品问答任何访客都能看到，交易单私聊只有买卖双方可见。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "dealId": 1,
    "itemId": 6,
    "itemTitle": "尤克里里 23 寸",
    "status": "COMPLETED",
    "dealPrice": 80.00,
    "peerNickname": "宋杰",
    "list": [
      { "id": 1, "mine": true, "senderNickname": "蒲承玺", "content": "师兄，周末下午…", "createdAt": "2026-10-10T21:47:00" }
    ]
  }
}
```

错误：`4003`（非交易双方）、`4004`（交易单不存在）。

#### 10.12 交易单私密会话（发送）

`POST /api/deals/{id}/messages`（仅该交易单的买家 / 卖家）

请求体：`{ "content": "今晚 7 点东风校区食堂门口见？" }`，长度 1~200 字；接收方自动取交易对手方。

说明：交易单为 `CANCELLED` 时会话转为只读；消息未读数计入 `GET /api/deals` 的 `unreadMessageCount` 并累加到顶部待办角标。

错误：`4001`（内容为空或超长）、`4003`、`4004`、`4005`（交易单已取消）。

---

### 十一、信用与治理模块（M5 信用与治理模块）

#### 11.1 提交评价

`POST /api/reviews`（登录）

请求体：`{ "itemId": 1, "rating": 5, "content": "教材很新，交易顺利" }`

校验（AC-11.1、AC-11.2、AC-11.6）：商品必须存在 `COMPLETED` 交易单；当前用户必须是该交易单的买家或卖家，对手方取交易单另一方；同一交易单中一方对另一方仅能评价一次。卖家「线下已售」（无交易单）的商品不可评价。

错误：`4003`（非交易参与方）、`4005`（重复评价）。

#### 11.2 用户评价列表

`GET /api/users/{userId}/reviews`（匿名）

响应 `data`（AC-11.4）：

```json
{
  "avgRating": 4.8,
  "reviewCount": 12,
  "list": [
    { "id": 1, "reviewerNickname": "宋杰", "rating": 5, "content": "教材很新", "createdAt": "2026-11-02T10:00:00" }
  ]
}
```

`GET /api/reviews/mine`（登录）

返回当前用户的评价活动（AC-11.4）：

```json
{
  "list": [
    { "id": 3, "itemId": 8, "itemTitle": "罗技鼠标 G102", "targetNickname": "宁素天", "rating": 5, "content": "交易顺利", "createdAt": "2026-11-02T10:00:00" }
  ],
  "pending": [
    { "itemId": 9, "itemTitle": "宿舍护眼小台灯", "targetNickname": "宋杰", "soldAt": "2026-11-01T18:00:00" }
  ]
}
```

`list` = 我发出的评价；`pending` = 我已成交但尚未评价的商品。

#### 11.3 提交举报

`POST /api/reports`（登录）

请求体：`{ "itemId": 1, "reason": "FAKE", "detail": "描述与实物不符" }`

校验（AC-11.3）：不能举报自己发布的商品；同一用户对同一商品仅能举报一次。

响应 `data`：`{ "id": 1, "status": "PENDING" }`。

`GET /api/reports/mine`（登录）

返回当前用户提交过的举报及处理结果，供信用中心「我的举报」页签使用：

```json
[
  { "id": 2, "itemId": 5, "itemTitle": "某违规商品", "reason": "FAKE", "status": "REJECTED", "createdAt": "2026-11-01T09:00:00", "handledAt": "2026-11-02T09:00:00" }
]
```

#### 11.4 管理后台：用户管理

`GET /api/admin/users`（管理员）

参数：`keyword`、`status`、`pageNum`、`pageSize`；返回用户列表（`id, studentNo, nickname, phone, role, status, createdAt`）。

`PATCH /api/admin/users/{id}/status`（管理员）

请求体：`{ "status": "DISABLED" }`（或 `ACTIVE`）；被禁用账号无法登录，登录接口返回 `4007`（AC-12.2）。

#### 11.5 管理后台：商品管理

`GET /api/admin/items`（管理员）

参数：`status`、`keyword`、`pageNum`、`pageSize`；返回全部商品（含下架 / 已售）。

`PATCH /api/admin/items/{id}/off-shelf`（管理员）

强制下架违规商品，商品状态置为 `OFF_SHELF` 并脱离集市列表（AC-12.3）。

#### 11.6 管理后台：举报处置

`GET /api/admin/reports`（管理员）

参数：`status`、`pageNum`、`pageSize`；返回举报记录列表。

`PATCH /api/admin/reports/{id}`（管理员）

请求体：`{ "action": "OFF_SHELF" }` 或 `{ "action": "REJECT" }`。

联动（AC-11.5、AC-12.4）：`OFF_SHELF` → 举报置为 `OFF_SHELF` 并强制下架商品；`REJECT` → 举报置为 `REJECTED`；处置人与处置时间留痕。

#### 11.7 管理后台：统计

复用 `GET /api/system/stats`（AC-12.5），后台首页直接展示四项指标。

---

### 十二、接口-需求追溯

| 模块 | 接口 | 覆盖需求 |
| :--- | :--- | :--- |
| M1 用户模块 | `/api/auth/*` | US-01 |
| M2 商品模块 | `/api/items*`、`/api/files/upload` | US-02 ~ US-05 |
| M3 互动 | `/api/items/{id}/messages`、`/api/messages/*` | US-09 |
| M3 交易 | `/api/bargains/*`、`/api/reservations/*`、`/api/deals/*`（含 `/{id}/messages` 私聊） | US-06、US-10 |
| M4 平台门户 | `/api/categories`、`/api/system/stats` | US-08 |
| M5 信用与治理 | `/api/reviews`、`/api/reports`、`/api/admin/*` | US-11、US-12 |
| M6 工程交付线 | 一键启动 / 停止脚本、`tests/api-e2e.ps1` | US-07 |
