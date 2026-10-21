// 登录 SonarQube 并截图仪表盘，作为代码质量报告的可读留档
// 用法（放在 frontend/scripts 下是为了能解析 playwright 依赖）：
//   cd frontend
//   $env:SONAR_PASSWORD='...'; node scripts/capture-sonar-dashboard.mjs
import { chromium } from 'playwright'
import { mkdir } from 'node:fs/promises'
import path from 'node:path'

const BASE = process.env.SONAR_URL || 'http://localhost:9000'
const PROJECT = process.env.SONAR_PROJECT || 'campushub-backend'
const USER = process.env.SONAR_USER || 'admin'
const PASSWORD = process.env.SONAR_PASSWORD

if (!PASSWORD) {
  console.error('缺少 SONAR_PASSWORD 环境变量（不会写入仓库，仅本次会话使用）')
  process.exit(2)
}

const stamp = new Date().toISOString().slice(0, 10)
const outDir = path.resolve(
  process.argv[2] || path.join(process.cwd(), '..', 'tests', 'reports', 'screenshots', stamp)
)
await mkdir(outDir, { recursive: true })

const browser = await chromium.launch()
const context = await browser.newContext({ viewport: { width: 1500, height: 950 }, deviceScaleFactor: 1 })
const page = await context.newPage()

async function login() {
  await page.goto(`${BASE}/sessions/new`, { waitUntil: 'domcontentloaded' })
  await page.locator('input[name="login"], #login').first().fill(USER)
  await page.locator('input[name="password"], #password').first().fill(PASSWORD)
  await page.locator('button[type="submit"], button:has-text("Log in")').first().click()
  await page.waitForURL(url => !url.pathname.startsWith('/sessions/new'), { timeout: 60000 })
}

await login()

await page.goto(`${BASE}/dashboard?id=${PROJECT}`, { waitUntil: 'domcontentloaded' })
await page.waitForTimeout(6000)
const overview = path.join(outDir, '09-SonarQube-代码质量总览.png')
await page.screenshot({ path: overview, fullPage: true })
console.log(`captured ${path.basename(overview)}`)

await page.goto(`${BASE}/project/issues?id=${PROJECT}&resolved=false`, { waitUntil: 'domcontentloaded' })
await page.waitForTimeout(5000)
const issues = path.join(outDir, '10-SonarQube-问题清单.png')
await page.screenshot({ path: issues, fullPage: false })
console.log(`captured ${path.basename(issues)}`)

await browser.close()
console.log(`\nSonarQube 截图留档完成：${outDir}`)
