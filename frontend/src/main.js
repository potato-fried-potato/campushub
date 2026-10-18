import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { openAuth } from './auth-dialog'
import { clearAuth } from './auth-store'

import App from './App.vue'
import router from './router'
import './styles/main.css'

// 每个请求自动带上 JWT
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// 统一处理请求失败：401 失效回登录框，其余给出明确提示，避免页面静默无反应
axios.interceptors.response.use(
  response => response,
  error => {
    const status = error.response ? error.response.status : 0
    if (status === 401) {
      clearAuth()
      openAuth('login')
      ElMessage.warning('登录状态已失效，请重新登录')
    } else if (status === 0) {
      ElMessage.error('无法连接服务器，请确认后端已启动')
    } else if (status >= 500) {
      ElMessage.error('服务器开小差了（HTTP ' + status + '），请稍后重试')
    } else {
      ElMessage.error('请求失败（HTTP ' + status + '）')
    }
    return Promise.reject(error)
  }
)

// 带上中文语言包，弹窗按钮和表格空态才是中文
createApp(App).use(router).use(ElementPlus, { locale: zhCn }).mount('#app')
