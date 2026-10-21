import { defineConfig } from '@playwright/test'

// 本机默认复用系统 Edge（免下载浏览器）；CI 设 PW_CHANNEL=bundled 用 Playwright 自带 Chromium
const channel = process.env.PW_CHANNEL === 'bundled' ? undefined : (process.env.PW_CHANNEL || 'msedge')

export default defineConfig({
  testDir: './e2e',
  timeout: 60000,
  expect: { timeout: 10000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:8080',
    channel,
    headless: true,
    viewport: { width: 1440, height: 900 },
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  },
  webServer: {
    command: 'cd ../backend && mvn -q spring-boot:run',
    url: 'http://localhost:8080/api/categories',
    reuseExistingServer: true,
    timeout: 120000
  }
})
