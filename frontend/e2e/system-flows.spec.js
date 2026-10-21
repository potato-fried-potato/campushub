import { test, expect } from '@playwright/test'

// 系统测试：按模块覆盖集市检索、发布、商品问答、预约、交易单私聊与信用中心
const API = 'http://localhost:8080/api'
const SELLER = { studentNo: '542412320201', password: '123456' }
const BUYER = { studentNo: '542412310217', password: '123456' }
const TINY_PNG_BASE64 =
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII='
const TINY_PNG = `data:image/png;base64,${TINY_PNG_BASE64}`

async function apiLogin(request, credentials) {
  const response = await request.post(`${API}/auth/login`, { data: credentials })
  const body = await response.json()
  expect(body.code).toBe(0)
  return body.data.token
}

async function publishItem(request, token, title, price = 66) {
  const response = await request.post(`${API}/items`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      title,
      description: 'Playwright 系统测试商品',
      price,
      originalPrice: price + 33,
      conditionLevel: 'NEARLY_NEW',
      campus: 'DONGFENG',
      categoryId: 1,
      images: [TINY_PNG]
    }
  })
  const body = await response.json()
  expect(body.code).toBe(0)
  return body.data.id
}

async function loginFromUserSide(page, credentials) {
  await page.goto('/')
  await page.locator('.ch-login-link').click()
  await page.getByPlaceholder('学号', { exact: true }).fill(credentials.studentNo)
  await page.getByPlaceholder('密码', { exact: true }).fill(credentials.password)
  await page.getByRole('button', { name: '登录', exact: true }).last().click()
  await expect(page.locator('.ch-user-chip')).toBeVisible()
}

test('集市关键词搜索与分类筛选联动', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('.ch-board-title')).toHaveText('闲置集市')
  await expect(page.locator('.ch-result-hint')).toContainText('件符合条件')

  await page.locator('.ch-search-input').fill('教材')
  await page.locator('.ch-search-btn').click()

  // 关键词落到路由 query，并出现「关键词」筛选行
  await expect(page).toHaveURL(/keyword=/)
  await expect(page.locator('.ch-filter-row', { hasText: '关键词' }).locator('.tag')).toHaveText('教材')

  // 分类 chip 切换后保持激活态
  const categoryChip = page.locator('.ch-cat-chip').nth(1)
  await categoryChip.click()
  await expect(categoryChip).toHaveClass(/active/)

  // 重置全部筛选后关键词行消失
  await page.locator('.ch-reset', { hasText: '重置全部筛选' }).click()
  await expect(page.locator('.ch-filter-row', { hasText: '关键词' })).toHaveCount(0)
})

test('发布闲置弹窗可完成发布并在我的发布中可见', async ({ page }) => {
  await loginFromUserSide(page, SELLER)
  const title = `UI发布商品${Date.now()}`

  await page.getByRole('button', { name: /发布闲置/ }).click()
  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog.locator('.el-dialog__title')).toHaveText('发布闲置')

  await dialog.getByPlaceholder('例如：九成新数据结构教材').fill(title)

  await dialog.locator('.el-select').click()
  await page.locator('.el-select-dropdown__item:visible').first().click()

  await dialog.locator('.el-input-number input').first().fill('66')

  await dialog.locator('input[type=file]').setInputFiles({
    name: 'item.png',
    mimeType: 'image/png',
    buffer: Buffer.from(TINY_PNG_BASE64, 'base64')
  })
  await expect(dialog.locator('.previews img')).toHaveCount(1)

  await dialog.getByRole('button', { name: '立即发布' }).click()
  await expect(dialog).toBeHidden()

  await page.goto('/#/mine')
  await expect(page.getByText(title)).toBeVisible()
})

test('商品详情可发布公开留言', async ({ page, request }) => {
  const sellerToken = await apiLogin(request, SELLER)
  const title = `UI留言商品${Date.now()}`
  const itemId = await publishItem(request, sellerToken, title)

  await loginFromUserSide(page, BUYER)
  await page.goto(`/#/item/${itemId}`)
  await expect(page.getByRole('heading', { name: title })).toBeVisible()

  const content = `请问还在吗 ${Date.now()}`
  const qa = page.locator('#qa')
  await qa.getByPlaceholder('向卖家提问，例如：还在吗？可以面交吗？').fill(content)
  await qa.getByRole('button', { name: '发送' }).click()

  await expect(qa.locator('.msg-item', { hasText: content })).toBeVisible()
})

test('商品详情可提交预约并落到交易中心', async ({ page, request }) => {
  const sellerToken = await apiLogin(request, SELLER)
  const buyerToken = await apiLogin(request, BUYER)
  const title = `UI预约商品${Date.now()}`
  const itemId = await publishItem(request, sellerToken, title, 120)

  await loginFromUserSide(page, BUYER)
  await page.goto(`/#/item/${itemId}`)

  await page.getByRole('button', { name: '预约购买' }).click()
  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog.locator('.el-dialog__title')).toHaveText('预约购买')
  await dialog.getByPlaceholder(/期望面交时间/).fill('2026-12-01T15:00:00')
  await dialog.getByPlaceholder(/备注/).fill('东风校区图书馆门口')
  await dialog.getByRole('button', { name: '提交预约' }).click()

  await expect(page.getByText('预约已提交')).toBeVisible()

  // 已提交的预约应能在「我发出的」中查到
  const sent = await request.get(`${API}/reservations?view=sent`, {
    headers: { Authorization: `Bearer ${buyerToken}` }
  })
  const sentBody = await sent.json()
  expect(sentBody.code).toBe(0)
  expect(sentBody.data.list.some(r => r.itemId === itemId)).toBe(true)
})

test('我的购买可联系卖家并在交易单私聊中发送消息', async ({ page, request }) => {
  const sellerToken = await apiLogin(request, SELLER)
  const buyerToken = await apiLogin(request, BUYER)
  const title = `UI私聊商品${Date.now()}`
  const itemId = await publishItem(request, sellerToken, title)

  const dealResponse = await request.post(`${API}/deals/direct`, {
    headers: { Authorization: `Bearer ${buyerToken}` },
    data: { itemId }
  })
  expect((await dealResponse.json()).code).toBe(0)

  await loginFromUserSide(page, BUYER)
  await page.goto('/#/purchases')

  const row = page.locator('.el-table__row', { hasText: title })
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: '联系卖家' }).click()

  const chatInput = page.getByPlaceholder('输入消息，例如：今晚 7 点东风校区食堂门口见？')
  await expect(chatInput).toBeVisible()

  const message = `今晚七点食堂门口见 ${Date.now()}`
  await chatInput.fill(message)
  await page.locator('.el-dialog:visible').getByRole('button', { name: '发送' }).click()
  await expect(page.locator('.el-dialog:visible .chat-text', { hasText: message })).toBeVisible()
})

test('信用中心展示基础分与计分规则', async ({ page }) => {
  await loginFromUserSide(page, BUYER)
  await page.goto('/#/credit')

  await expect(page.getByRole('heading', { name: '我的信用' })).toBeVisible()
  await expect(page.getByText(/基础分 80/).first()).toBeVisible()
  await expect(page.getByText(/计分规则：基础分 80/)).toBeVisible()
})
