import { reactive } from 'vue'

// 全局登录 / 注册弹窗状态，任何页面都能唤起
export const authDialog = reactive({ visible: false, tab: 'login' })

export function openAuth(tab = 'login') {
  authDialog.tab = tab
  authDialog.visible = true
}
