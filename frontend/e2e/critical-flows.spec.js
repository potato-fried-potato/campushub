import { test, expect } from '@playwright/test'

const API = 'http://localhost:8080/api'
const SELLER = { studentNo: '542412320201', password: '123456' }
const BUYER = { studentNo: '542412310217', password: '123456' }
const TINY_PNG = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII='

async function apiLogin(request, credentials) {
  const response = await request.post(`${API}/auth/login`, { data: credentials })
  const body = await response.json()
  expect(body.code).toBe(0)
  return body.data.token
}

async function publishItem(request, token, title) {
  const response = await request.post(`${API}/items`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      title,
      description: 'Playwright UI 测试商品',
      price: 66,
      originalPrice: 99,
      conditionLevel: 'NEW',
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

test('买家下单 → 交易中心确认成交 → 我的购买可见', async ({ page, request }) => {
  const sellerToken = await apiLogin(request, SELLER)
  const title = `Playwright测试商品${Date.now()}`
  const itemId = await publishItem(request, sellerToken, title)

  await loginFromUserSide(page, BUYER)
  await page.goto(`/#/item/${itemId}`)
  await expect(page.getByRole('heading', { name: title })).toBeVisible()

  await page.getByRole('button', { name: /立即购买/ }).click()
  await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
  await expect(page.getByText('待成交交易单')).toBeVisible()

  await page.goto('/#/trade')
  await page.locator('.el-radio-button', { hasText: '我发出的' }).click()
  await page.locator('.el-tabs__item', { hasText: '交易单' }).click()

  const row = page.locator('.el-table__row', { hasText: title })
  await expect(row).toBeVisible()
  await row.getByRole('button', { name: '确认成交' }).click()
  await page.locator('.el-message-box').getByRole('button', { name: '确认成交' }).click()
  await expect(row.getByText('已完成')).toBeVisible()

  await page.goto('/#/purchases')
  await expect(page.getByText(title)).toBeVisible()
  await expect(page.getByText('已完成').first()).toBeVisible()
})

test('管理员从独立入口登录后可进后台，用户端无交易入口', async ({ page }) => {
  await page.goto('/#/admin/login')
  await page.getByPlaceholder('管理员账号').fill('admin')
  await page.getByPlaceholder('密码', { exact: true }).fill('123456')
  await page.getByRole('button', { name: '登录管理后台' }).click()

  await expect(page).toHaveURL(/#\/admin\/overview/)
  await expect(page.getByText('数据概览').first()).toBeVisible()

  await page.getByRole('link', { name: /返回集市/ }).click()
  await expect(page.getByRole('link', { name: '管理后台' })).toBeVisible()
  await expect(page.getByRole('button', { name: /发布闲置/ })).toHaveCount(0)

  await page.goto('/#/item/4')
  await expect(page.getByText('管理员账号仅用于后台管理，不参与用户交易')).toBeVisible()
  await expect(page.getByRole('button', { name: /立即购买/ })).toHaveCount(0)
})

test('管理员账号从用户端登录会被引导到管理端', async ({ page }) => {
  await page.goto('/')
  await page.locator('.ch-login-link').click()
  await page.getByPlaceholder('学号', { exact: true }).fill('admin')
  await page.getByPlaceholder('密码', { exact: true }).fill('123456')
  await page.getByRole('button', { name: '登录', exact: true }).last().click()

  await expect(page).toHaveURL(/#\/admin\/login/)
  await expect(page.getByText('管理员账号请从管理后台入口登录')).toBeVisible()
})

test('砍价弹窗展示可出价区间并拦截低于 8 折', async ({ page }) => {
  await loginFromUserSide(page, BUYER)
  await page.goto('/#/item/4')
  await page.getByRole('button', { name: '我要出价' }).click()

  await expect(page.getByText(/可出价 ￥12\.00 ~ ￥14\.99/)).toBeVisible()
  await page.getByPlaceholder('你的出价金额').fill('10')
  await page.getByRole('button', { name: '提交出价' }).click()
  await expect(page.getByText('出价不能低于挂牌价 8 折（￥12.00）')).toBeVisible()
})
