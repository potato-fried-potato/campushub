import { reactive } from 'vue'

// 交易单私密会话：全局只挂一个弹窗，任意页面调用 openDealChat 即可打开
export const dealChat = reactive({
  visible: false,
  dealId: null,
  itemTitle: '',
  status: '',
  // 会话被读取后回调，用来刷新顶部的待办角标
  onRead: null
})

export function openDealChat(deal) {
  if (!deal) return
  dealChat.dealId = deal.id || deal.dealId
  dealChat.itemTitle = deal.itemTitle || ''
  dealChat.status = deal.status || ''
  dealChat.visible = true
}
