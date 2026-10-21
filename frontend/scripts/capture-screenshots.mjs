// 关键页面截图留档：为 docs/06 的「手工复核留档截图」提供真实证据
// 用法：cd frontend && node scripts/capture-screenshots.mjs [输出目录]
import { chromium } from 'playwright'
import { mkdir } from 'node:fs/promises'
import path from 'node:path'

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080'
const stamp = new Date().toISOString().slice(0, 10)
const outDir = path.resolve(
  process.argv[2] || path.join(process.cwd(), '..', 'tests', 'reports', 'screenshots', stamp)
)

const SELLER = { studentNo: '542412320201', password: '123456' }
const BUYER = { studentNo: '542412310217', password: '123456' }

await mkdir(outDir, { recursive: true })

const browser = await chromium.launch()
const shots = []

async function newPage() {
  const context = await browser.newContext({
    viewport: { width: 1366, height: 850 },
    deviceScaleFactor: 1
  })
  return { context, page: await context.newPage() }
}

async function loginFromHeader(page, credentials) {
  await page.goto(`${BASE_URL}/`)
  await page.locator('.ch-login-link').click()
  await page.getByPlaceholder('学号', { exact: true }).fill(credentials.studentNo)
  await page.getByPlaceholder('密码', { exact: true }).fill(credentials.password)
  await page.getByRole('button', { name: '登录', exact: true }).last().click()
  await page.locator('.ch-user-chip').waitFor({ state: 'visible' })
}

async function shot(page, name) {
  const file = path.join(outDir, name)
  await page.waitForTimeout(600)
  await page.screenshot({ path: file, type: 'jpeg', quality: 72, fullPage: false })
  shots.push(name)
  console.log(`captured ${name}`)
}

// 1. 集市首页（匿名）
{
  const { context, page } = await newPage()
  await page.goto(`${BASE_URL}/`)
  await page.locator('.ch-goods-grid').waitFor({ state: 'visible' })
  await shot(page, '01-集市首页.jpg')
  await context.close()
}

// 2. 商品详情与商品问答（买家）
{
  const { context, page } = await newPage()
  await loginFromHeader(page, BUYER)
  await page.goto(`${BASE_URL}/#/item/4`)
  await page.locator('#qa').waitFor({ state: 'visible' })
  await shot(page, '02-商品详情与商品问答.jpg')
  await context.close()
}

// 3. 发布闲置弹窗（卖家）
{
  const { context, page } = await newPage()
  await loginFromHeader(page, SELLER)
  await page.getByRole('button', { name: /发布闲置/ }).click()
  await page.locator('.el-dialog:visible').waitFor({ state: 'visible' })
  await shot(page, '03-发布闲置弹窗.jpg')
  await context.close()
}

// 4. 交易中心（买家）
{
  const { context, page } = await newPage()
  await loginFromHeader(page, BUYER)
  await page.goto(`${BASE_URL}/#/trade`)
  await page.locator('.el-tabs').first().waitFor({ state: 'visible' })
  await shot(page, '04-交易中心.jpg')
  await context.close()
}

// 5. 我的购买（买家）
{
  const { context, page } = await newPage()
  await loginFromHeader(page, BUYER)
  await page.goto(`${BASE_URL}/#/purchases`)
  await page.locator('.page-title').waitFor({ state: 'visible' })
  await shot(page, '05-我的购买.jpg')
  await context.close()
}

// 6. 信用中心（买家）
{
  const { context, page } = await newPage()
  await loginFromHeader(page, BUYER)
  await page.goto(`${BASE_URL}/#/credit`)
  await page.locator('.page-title').waitFor({ state: 'visible' })
  await shot(page, '06-信用中心.jpg')
  await context.close()
}

// 7. 管理端独立登录页
{
  const { context, page } = await newPage()
  await page.goto(`${BASE_URL}/#/admin/login`)
  await page.getByPlaceholder('管理员账号').waitFor({ state: 'visible' })
  await shot(page, '07-管理端登录页.jpg')
  await context.close()
}

// 8. 管理后台数据概览
{
  const { context, page } = await newPage()
  await page.goto(`${BASE_URL}/#/admin/login`)
  await page.getByPlaceholder('管理员账号').fill('admin')
  await page.getByPlaceholder('密码', { exact: true }).fill('123456')
  await page.getByRole('button', { name: '登录管理后台' }).click()
  await page.waitForURL(/#\/admin\/overview/)
  await shot(page, '08-管理后台数据概览.jpg')
  await context.close()
}

await browser.close()
console.log(`\n截图留档完成：${outDir}（${shots.length} 张）`)
