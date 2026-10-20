import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

const tinyImage = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII='

function jsonBody(value) {
  return { mode: 'raw', raw: JSON.stringify(value) }
}

function req(name, method, url, { token, body, tests = [], pre = [], form } = {}) {
  const header = []
  if (body) header.push({ key: 'Content-Type', value: 'application/json' })
  if (token) header.push({ key: 'Authorization', value: `Bearer {{${token}}}` })
  const item = {
    name,
    request: { method, header, url }
  }
  if (body) item.request.body = jsonBody(body)
  if (form) item.request.body = form
  const event = []
  if (pre.length) event.push({ listen: 'prerequest', script: { type: 'text/javascript', exec: pre } })
  if (tests.length) event.push({ listen: 'test', script: { type: 'text/javascript', exec: tests } })
  if (event.length) item.event = event
  return item
}

const code = (n) => [
  'const body = pm.response.json();',
  `pm.test('业务码 ${n}', () => { pm.expect(body.code).to.eql(${n}); });`
]

const codeAnd = (n, lines) => [...code(n), ...lines]

const group = (name, items) => ({ name, item: items })

const collection = {
  info: {
    name: 'CampusHub API 全量回归（Apifox 导入）',
    description: '覆盖全部接口与关键边界。执行前先 db/init-db.bat 重置种子库。',
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'
  },
  variable: [
    { key: 'baseUrl', value: 'http://localhost:8080/api' },
    { key: 'sellerNo', value: '542412320201' },
    { key: 'buyerNo', value: '542412310217' },
    { key: 'adminNo', value: 'admin' },
    { key: 'password', value: '123456' },
    { key: 'tinyImage', value: tinyImage },
    { key: 'sellerToken', value: '' },
    { key: 'buyerToken', value: '' },
    { key: 'adminToken', value: '' },
    { key: 'newUserId', value: '' },
    { key: 'newStudentNo', value: '' },
    { key: 'itemA', value: '' },
    { key: 'itemB', value: '' },
    { key: 'itemC', value: '' },
    { key: 'itemD', value: '' },
    { key: 'itemE', value: '' },
    { key: 'bargainA', value: '' },
    { key: 'bargainReject', value: '' },
    { key: 'bargainCancel', value: '' },
    { key: 'dealA', value: '' },
    { key: 'dealB', value: '' },
    { key: 'dealC', value: '' },
    { key: 'dealD', value: '' },
    { key: 'reservation1', value: '' },
    { key: 'reservation2', value: '' },
    { key: 'reservation3', value: '' },
    { key: 'messageId', value: '' },
    { key: 'reportId', value: '' }
  ],
  item: [
    group('00 认证', [
      req('注册新用户', 'POST', '{{baseUrl}}/auth/register', {
        pre: ["pm.collectionVariables.set('newStudentNo', 'apifox' + Date.now());"],
        body: { studentNo: '{{newStudentNo}}', password: '{{password}}', nickname: 'Apifox用户', phone: '13800001234' },
        tests: codeAnd(0, [
          'pm.collectionVariables.set("newUserId", body.data.user.id);',
          'pm.test("返回 JWT", () => pm.expect(body.data.token).to.be.a("string"));'
        ])
      }),
      req('重复注册被拒', 'POST', '{{baseUrl}}/auth/register', {
        body: { studentNo: '{{newStudentNo}}', password: '{{password}}', nickname: '重复', phone: '13800001235' },
        tests: code(4005)
      }),
      req('登录-卖家', 'POST', '{{baseUrl}}/auth/login', {
        body: { studentNo: '{{sellerNo}}', password: '{{password}}' },
        tests: codeAnd(0, ['pm.collectionVariables.set("sellerToken", body.data.token);'])
      }),
      req('登录-买家', 'POST', '{{baseUrl}}/auth/login', {
        body: { studentNo: '{{buyerNo}}', password: '{{password}}' },
        tests: codeAnd(0, ['pm.collectionVariables.set("buyerToken", body.data.token);'])
      }),
      req('登录-管理员', 'POST', '{{baseUrl}}/auth/login', {
        body: { studentNo: '{{adminNo}}', password: '{{password}}' },
        tests: codeAnd(0, ['pm.collectionVariables.set("adminToken", body.data.token);', 'pm.test("角色为 ADMIN", () => pm.expect(body.data.user.role).to.eql("ADMIN"));'])
      }),
      req('错误密码登录', 'POST', '{{baseUrl}}/auth/login', {
        body: { studentNo: '{{sellerNo}}', password: 'wrong-password' },
        tests: code(4002)
      }),
      req('当前用户信息', 'GET', '{{baseUrl}}/auth/me', {
        token: 'sellerToken',
        tests: codeAnd(0, ['pm.test("返回学号", () => pm.expect(body.data.studentNo).to.be.a("string"));'])
      }),
      req('登出', 'POST', '{{baseUrl}}/auth/logout', { token: 'buyerToken', tests: code(0) }),
      req('分类列表', 'GET', '{{baseUrl}}/categories', {
        tests: codeAnd(0, ['pm.test("6 个分类", () => pm.expect(body.data.length).to.eql(6));'])
      }),
      req('平台统计', 'GET', '{{baseUrl}}/system/stats', {
        tests: codeAnd(0, ['pm.test("四项指标", () => { pm.expect(body.data.onSaleCount).to.be.a("number"); pm.expect(body.data.soldCount).to.be.a("number"); });'])
      })
    ]),
    group('01 商品', [
      ...[['itemA', 100], ['itemB', 120], ['itemC', 140], ['itemD', 160], ['itemE', 180]].map(([key, price]) =>
        req(`发布商品 ${key}`, 'POST', '{{baseUrl}}/items', {
          token: 'sellerToken',
          body: {
            title: `Apifox-${key}-{{$timestamp}}`, description: 'Apifox 全量回归', price,
            originalPrice: price * 2, conditionLevel: 'NEW', campus: 'DONGFENG', categoryId: 1,
            images: ['{{tinyImage}}']
          },
          tests: codeAnd(0, [`pm.collectionVariables.set("${key}", body.data.id);`])
        })
      ),
      req('集市关键词检索', 'GET', '{{baseUrl}}/items?keyword=Apifox&pageSize=50', {
        tests: codeAnd(0, ['pm.test("返回列表", () => pm.expect(body.data.list).to.be.an("array"));'])
      }),
      req('集市已售视图', 'GET', '{{baseUrl}}/items?status=SOLD', { tests: code(0) }),
      req('详情隐私与砍价下限', 'GET', '{{baseUrl}}/items/{{itemA}}', {
        token: 'buyerToken',
        tests: codeAnd(0, [
          'pm.test("不暴露手机号", () => pm.expect(body.data.seller.phone).to.be.undefined);',
          'pm.test("砍价下限 8 折", () => pm.expect(Number(body.data.bargainFloor)).to.eql(80));',
          'pm.test("可立即购买", () => pm.expect(body.data.canDirectBuy).to.eql(true));'
        ])
      }),
      req('我的发布', 'GET', '{{baseUrl}}/items/mine?pageSize=50', {
        token: 'sellerToken',
        tests: codeAnd(0, ['pm.test("包含新发布商品", () => { const id = Number(pm.collectionVariables.get("itemA")); pm.expect(body.data.list.some(i => i.id === id)).to.eql(true); });'])
      }),
      req('下架商品', 'PATCH', '{{baseUrl}}/items/{{itemD}}/status', { token: 'sellerToken', body: { status: 'OFF_SHELF' }, tests: code(0) }),
      req('重新上架', 'PATCH', '{{baseUrl}}/items/{{itemD}}/status', { token: 'sellerToken', body: { status: 'ON_SALE' }, tests: code(0) }),
      req('越权改他人商品', 'PATCH', '{{baseUrl}}/items/{{itemA}}/status', { token: 'buyerToken', body: { status: 'OFF_SHELF' }, tests: code(4003) }),
      req('线下已售', 'PATCH', '{{baseUrl}}/items/{{itemE}}/status', { token: 'sellerToken', body: { status: 'SOLD' }, tests: code(0) }),
      req('线下已售无交易单', 'GET', '{{baseUrl}}/items/{{itemE}}', {
        tests: codeAnd(0, ['pm.test("已售且无交易单", () => { pm.expect(body.data.status).to.eql("SOLD"); pm.expect(body.data.dealStatus).to.be.null; });'])
      }),
      req('线下已售不可评价', 'POST', '{{baseUrl}}/reviews', {
        token: 'buyerToken', body: { itemId: '{{itemE}}', rating: 5, content: '线下已售' }, tests: code(4003)
      }),
      req('图片上传（手动，需文件）', 'POST', '{{baseUrl}}/files/upload', {
        token: 'sellerToken',
        form: {
          mode: 'formdata',
          formdata: [{ key: 'file', type: 'file', src: 'tests/apifox/tiny.png' }]
        },
        tests: code(0)
      })
    ]),
    group('02 留言', [
      req('匿名问答列表', 'GET', '{{baseUrl}}/items/{{itemA}}/messages', { tests: code(0) }),
      req('买家留言', 'POST', '{{baseUrl}}/items/{{itemA}}/messages', {
        token: 'buyerToken', body: { content: 'Apifox留言{{$timestamp}}' }, tests: code(0)
      }),
      req('保存留言话题 id', 'GET', '{{baseUrl}}/items/{{itemA}}/messages', {
        tests: codeAnd(0, ['pm.collectionVariables.set("messageId", body.data[0].id);', 'pm.test("存在留言", () => pm.expect(body.data.length).to.be.above(0));'])
      }),
      req('卖家回复', 'POST', '{{baseUrl}}/items/{{itemA}}/messages', {
        token: 'sellerToken', body: { content: '卖家回复', parentId: '{{messageId}}' }, tests: code(0)
      }),
      req('我的留言-买家', 'GET', '{{baseUrl}}/messages/mine?view=sent', { token: 'buyerToken', tests: code(0) }),
      req('我的留言-卖家', 'GET', '{{baseUrl}}/messages/mine?view=received', { token: 'sellerToken', tests: code(0) }),
      req('标记单条已读', 'PATCH', '{{baseUrl}}/messages/{{messageId}}/read', { token: 'sellerToken', tests: code(0) }),
      req('标记商品留言已读', 'PATCH', '{{baseUrl}}/messages/item/{{itemA}}/read', { token: 'sellerToken', tests: code(0) }),
      req('空白留言被拒', 'POST', '{{baseUrl}}/items/{{itemA}}/messages', { token: 'buyerToken', body: { content: '   ' }, tests: code(4001) }),
      req('匿名留言 401', 'POST', '{{baseUrl}}/items/{{itemA}}/messages', {
        body: { content: '匿名' }, tests: ['pm.test("HTTP 401", () => pm.response.to.have.status(401));']
      })
    ]),
    group('03 砍价', [
      req('低于 8 折被拒', 'POST', '{{baseUrl}}/bargains', { token: 'buyerToken', body: { itemId: '{{itemA}}', offerPrice: 79 }, tests: code(4001) }),
      req('8 折出价', 'POST', '{{baseUrl}}/bargains', {
        token: 'buyerToken', body: { itemId: '{{itemA}}', offerPrice: 80 }, tests: codeAnd(0, ['pm.collectionVariables.set("bargainA", body.data.id);'])
      }),
      req('重复出价被拒', 'POST', '{{baseUrl}}/bargains', { token: 'buyerToken', body: { itemId: '{{itemA}}', offerPrice: 80 }, tests: code(4005) }),
      req('高于挂牌价被拒', 'POST', '{{baseUrl}}/bargains', { token: 'buyerToken', body: { itemId: '{{itemD}}', offerPrice: 160 }, tests: code(4001) }),
      req('砍价列表-买家', 'GET', '{{baseUrl}}/bargains?view=sent', { token: 'buyerToken', tests: code(0) }),
      req('买家接受自己的出价被拒', 'POST', '{{baseUrl}}/bargains/{{bargainA}}/accept', { token: 'buyerToken', body: {}, tests: code(4003) }),
      req('卖家接受出价', 'POST', '{{baseUrl}}/bargains/{{bargainA}}/accept', { token: 'sellerToken', body: {}, tests: code(0) }),
      req('待成交锁定', 'GET', '{{baseUrl}}/items/{{itemA}}', {
        token: 'buyerToken', tests: codeAnd(0, ['pm.test("待成交", () => { pm.expect(body.data.reserved).to.eql(true); pm.expect(body.data.dealStatus).to.eql("PENDING"); });'])
      }),
      req('锁定期再出价被拒', 'POST', '{{baseUrl}}/bargains', { token: 'buyerToken', body: { itemId: '{{itemA}}', offerPrice: 80 }, tests: code(4005) }),
      req('查询砍价交易单', 'GET', '{{baseUrl}}/deals?view=received', {
        token: 'sellerToken', tests: codeAnd(0, ['const itemId = Number(pm.collectionVariables.get("itemA")); const deal = body.data.list.find(d => d.itemId === itemId); pm.test("找到交易单", () => pm.expect(deal).to.not.be.undefined); if (deal) pm.collectionVariables.set("dealA", deal.id);'])
      }),
      req('拒绝出价', 'POST', '{{baseUrl}}/bargains', { token: 'buyerToken', body: { itemId: '{{itemD}}', offerPrice: 130 }, tests: codeAnd(0, ['pm.collectionVariables.set("bargainReject", body.data.id);']) }),
      req('卖家拒绝出价', 'POST', '{{baseUrl}}/bargains/{{bargainReject}}/reject', { token: 'sellerToken', body: {}, tests: code(0) }),
      req('取消出价', 'POST', '{{baseUrl}}/bargains', { token: 'buyerToken', body: { itemId: '{{itemD}}', offerPrice: 130 }, tests: codeAnd(0, ['pm.collectionVariables.set("bargainCancel", body.data.id);']) }),
      req('买家取消出价', 'POST', '{{baseUrl}}/bargains/{{bargainCancel}}/cancel', { token: 'buyerToken', body: {}, tests: code(0) })
    ]),
    group('04 交易单', [
      req('立即购买', 'POST', '{{baseUrl}}/deals/direct', { token: 'buyerToken', body: { itemId: '{{itemB}}' }, tests: codeAnd(0, ['pm.collectionVariables.set("dealB", body.data.id);']) }),
      req('重复立即购买被拒', 'POST', '{{baseUrl}}/deals/direct', { token: 'buyerToken', body: { itemId: '{{itemB}}' }, tests: code(4005) }),
      req('卖家确认成交', 'POST', '{{baseUrl}}/deals/{{dealB}}/complete', { token: 'sellerToken', body: {}, tests: code(0) }),
      req('成交后详情', 'GET', '{{baseUrl}}/items/{{itemB}}', {
        token: 'buyerToken', tests: codeAnd(0, ['pm.test("已售且可评价", () => { pm.expect(body.data.status).to.eql("SOLD"); pm.expect(body.data.dealStatus).to.eql("COMPLETED"); pm.expect(body.data.canReview).to.eql(true); });'])
      }),
      req('买家评价', 'POST', '{{baseUrl}}/reviews', { token: 'buyerToken', body: { itemId: '{{itemB}}', rating: 5, content: 'Apifox评价' }, tests: code(0) }),
      req('重复评价被拒', 'POST', '{{baseUrl}}/reviews', { token: 'buyerToken', body: { itemId: '{{itemB}}', rating: 4, content: '重复评价' }, tests: code(4005) }),
      req('立即购买待取消', 'POST', '{{baseUrl}}/deals/direct', { token: 'buyerToken', body: { itemId: '{{itemD}}' }, tests: codeAnd(0, ['pm.collectionVariables.set("dealD", body.data.id);']) }),
      req('取消交易单', 'POST', '{{baseUrl}}/deals/{{dealD}}/cancel', { token: 'buyerToken', body: {}, tests: code(0) }),
      req('取消后解锁', 'GET', '{{baseUrl}}/items/{{itemD}}', {
        token: 'buyerToken', tests: codeAnd(0, ['pm.test("回到在售", () => { pm.expect(body.data.status).to.eql("ON_SALE"); pm.expect(body.data.reserved).to.eql(false); });'])
      }),
      req('已取消交易单私聊只读', 'POST', '{{baseUrl}}/deals/{{dealD}}/messages', { token: 'buyerToken', body: { content: '已取消' }, tests: code(4005) }),
      req('确认砍价交易单', 'POST', '{{baseUrl}}/deals/{{dealA}}/complete', { token: 'sellerToken', body: {}, tests: code(0) }),
      req('卖家评价买家', 'POST', '{{baseUrl}}/reviews', { token: 'sellerToken', body: { itemId: '{{itemA}}', rating: 5, content: '买家守时' }, tests: code(0) })
    ]),
    group('05 预约', [
      req('提交预约', 'POST', '{{baseUrl}}/reservations', {
        token: 'buyerToken', body: { itemId: '{{itemC}}', expectTime: '2026-12-01T15:00:00', remark: '图书馆门口' },
        tests: codeAnd(0, ['pm.collectionVariables.set("reservation1", body.data.id);'])
      }),
      req('预约列表-买家', 'GET', '{{baseUrl}}/reservations?view=sent', { token: 'buyerToken', tests: code(0) }),
      req('重复预约被拒', 'POST', '{{baseUrl}}/reservations', { token: 'buyerToken', body: { itemId: '{{itemC}}' }, tests: code(4005) }),
      req('确认预约', 'POST', '{{baseUrl}}/reservations/{{reservation1}}/confirm', { token: 'sellerToken', body: {}, tests: code(0) }),
      req('预约锁定详情', 'GET', '{{baseUrl}}/items/{{itemC}}', {
        token: 'buyerToken', tests: codeAnd(0, ['pm.test("预约锁定", () => { pm.expect(body.data.reserved).to.eql(true); pm.expect(body.data.dealSource).to.eql("RESERVATION"); });'])
      }),
      req('查询预约交易单', 'GET', '{{baseUrl}}/deals?view=received', {
        token: 'sellerToken', tests: codeAnd(0, ['const itemId = Number(pm.collectionVariables.get("itemC")); const deal = body.data.list.find(d => d.itemId === itemId); pm.test("找到预约交易单", () => pm.expect(deal).to.not.be.undefined); if (deal) pm.collectionVariables.set("dealC", deal.id);'])
      }),
      req('取消预约交易单', 'POST', '{{baseUrl}}/deals/{{dealC}}/cancel', { token: 'sellerToken', body: {}, tests: code(0) }),
      req('取消预约后解锁', 'GET', '{{baseUrl}}/items/{{itemC}}', { tests: codeAnd(0, ['pm.test("回到在售", () => pm.expect(body.data.status).to.eql("ON_SALE"));']) }),
      req('再次预约用于拒绝', 'POST', '{{baseUrl}}/reservations', { token: 'buyerToken', body: { itemId: '{{itemC}}' }, tests: codeAnd(0, ['pm.collectionVariables.set("reservation2", body.data.id);']) }),
      req('卖家拒绝预约', 'POST', '{{baseUrl}}/reservations/{{reservation2}}/reject', { token: 'sellerToken', body: {}, tests: code(0) }),
      req('再次预约用于取消', 'POST', '{{baseUrl}}/reservations', { token: 'buyerToken', body: { itemId: '{{itemC}}' }, tests: codeAnd(0, ['pm.collectionVariables.set("reservation3", body.data.id);']) }),
      req('买家取消预约', 'POST', '{{baseUrl}}/reservations/{{reservation3}}/cancel', { token: 'buyerToken', body: {}, tests: code(0) })
    ]),
    group('06 私聊、评价与举报', [
      req('买家发私聊', 'POST', '{{baseUrl}}/deals/{{dealA}}/messages', { token: 'buyerToken', body: { content: 'Apifox私聊' }, tests: code(0) }),
      req('卖家读私聊', 'GET', '{{baseUrl}}/deals/{{dealA}}/messages', {
        token: 'sellerToken', tests: codeAnd(0, ['pm.test("有消息", () => pm.expect(body.data.list.length).to.be.above(0));'])
      }),
      req('管理员读私聊被拒', 'GET', '{{baseUrl}}/deals/{{dealA}}/messages', { token: 'adminToken', tests: code(4003) }),
      req('非交易方确认成交被拒', 'POST', '{{baseUrl}}/deals/{{dealA}}/complete', { token: 'adminToken', body: {}, tests: code(4003) }),
      req('用户评价列表', 'GET', '{{baseUrl}}/users/2/reviews', { tests: code(0) }),
      req('我的评价', 'GET', '{{baseUrl}}/reviews/mine', { token: 'buyerToken', tests: code(0) }),
      req('提交举报', 'POST', '{{baseUrl}}/reports', {
        token: 'buyerToken', body: { itemId: '{{itemD}}', reason: 'FAKE', detail: 'Apifox举报' },
        tests: codeAnd(0, ['pm.collectionVariables.set("reportId", body.data.id);'])
      }),
      req('重复举报被拒', 'POST', '{{baseUrl}}/reports', { token: 'buyerToken', body: { itemId: '{{itemD}}', reason: 'FAKE' }, tests: code(4005) }),
      req('我的举报', 'GET', '{{baseUrl}}/reports/mine', { token: 'buyerToken', tests: code(0) })
    ]),
    group('07 管理端与权限', [
      req('管理员用户列表', 'GET', '{{baseUrl}}/admin/users', { token: 'adminToken', tests: code(0) }),
      req('禁用新用户', 'PATCH', '{{baseUrl}}/admin/users/{{newUserId}}/status', { token: 'adminToken', body: { status: 'DISABLED' }, tests: code(0) }),
      req('禁用用户登录失败', 'POST', '{{baseUrl}}/auth/login', { body: { studentNo: '{{newStudentNo}}', password: '{{password}}' }, tests: code(4007) }),
      req('启用新用户', 'PATCH', '{{baseUrl}}/admin/users/{{newUserId}}/status', { token: 'adminToken', body: { status: 'ACTIVE' }, tests: code(0) }),
      req('管理员商品列表', 'GET', '{{baseUrl}}/admin/items', { token: 'adminToken', tests: code(0) }),
      req('强制下架商品', 'PATCH', '{{baseUrl}}/admin/items/{{itemD}}/off-shelf', { token: 'adminToken', body: {}, tests: code(0) }),
      req('下架后商品不可见', 'GET', '{{baseUrl}}/items/{{itemD}}', { tests: codeAnd(0, ['pm.test("已下架", () => pm.expect(body.data.status).to.eql("OFF_SHELF"));']) }),
      req('管理员举报列表', 'GET', '{{baseUrl}}/admin/reports', { token: 'adminToken', tests: code(0) }),
      req('举报处置-下架', 'PATCH', '{{baseUrl}}/admin/reports/{{reportId}}', { token: 'adminToken', body: { action: 'OFF_SHELF' }, tests: code(0) }),
      req('管理员调用用户写接口被拒', 'POST', '{{baseUrl}}/bargains', { token: 'adminToken', body: { itemId: 1, offerPrice: 22 }, tests: code(4003) }),
      req('管理员只读浏览集市', 'GET', '{{baseUrl}}/items?pageSize=3', { token: 'adminToken', tests: code(0) }),
      req('普通用户访问管理接口被拒', 'GET', '{{baseUrl}}/admin/users', { token: 'sellerToken', tests: code(4003) }),
      req('未登录访问受保护接口', 'GET', '{{baseUrl}}/items/mine', {
        tests: ['pm.test("HTTP 401", () => pm.response.to.have.status(401));']
      })
    ])
  ]
}

const out = path.join(__dirname, 'campushub-apifox.postman_collection.json')
fs.writeFileSync(out, JSON.stringify(collection, null, 2), 'utf8')
const count = collection.item.reduce((sum, g) => sum + g.item.length, 0)
console.log(`generated ${out} (${count} requests)`)
