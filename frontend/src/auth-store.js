import { reactive } from 'vue'

// 全站唯一的登录态来源：登录/退出后各页面自动更新，无需刷新
function loadUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (e) {
    return null
  }
}

export const auth = reactive({ user: loadUser() })

export function setAuth(data) {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify(data.user))
  auth.user = data.user
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  auth.user = null
}
