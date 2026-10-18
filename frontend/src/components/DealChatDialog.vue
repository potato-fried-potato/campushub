<template>
  <el-dialog
    v-model="dealChat.visible"
    :title="'与 ' + (info.peerNickname || '对方') + ' 沟通'"
    width="520px"
    align-center
    @open="load"
  >
    <div v-if="info.itemTitle" class="chat-item">
      <span class="chat-item-title">{{ info.itemTitle }}</span>
      <span class="tag" :class="tagClass(info.status)">{{ statusText(info.status) }}</span>
      <span style="color: var(--text-muted); font-size: 12px">成交价 ￥{{ info.dealPrice }}</span>
    </div>
    <div class="chat-hint">仅你和交易对方可见，用来约定面交时间、地点等交易细节。</div>

    <div class="chat-body" v-loading="loading">
      <div v-for="m in info.list" :key="m.id" class="chat-row" :class="{ mine: m.mine }">
        <div class="chat-bubble">
          <div class="chat-text">{{ m.content }}</div>
          <div class="chat-time">{{ m.senderNickname }} · {{ formatTime(m.createdAt) }}</div>
        </div>
      </div>
      <div class="empty-state" v-if="!loading && info.list.length === 0">
        <div class="empty-title">还没有聊过</div>
        <div>约个面交时间、确认地点都可以在这里说</div>
      </div>
    </div>

    <div v-if="info.status === 'CANCELLED'" class="chat-hint">交易单已取消，会话转为只读。</div>
    <div v-else style="display: flex; gap: 10px; margin-top: 12px">
      <el-input
        v-model="content"
        placeholder="输入消息，例如：今晚 7 点东风校区食堂门口见？"
        maxlength="200"
        @keyup.enter="send"
      />
      <el-button type="primary" :loading="sending" @click="send">发送</el-button>
    </div>
  </el-dialog>
</template>

<script>
import axios from 'axios'
import { dealChat } from '../deal-chat'

export default {
  name: 'DealChatDialog',
  data() {
    return {
      dealChat,
      info: { peerNickname: '', itemTitle: '', dealPrice: '', status: '', list: [] },
      content: '',
      loading: false,
      sending: false,
      timer: null
    }
  },
  watch: {
    'dealChat.visible'(visible) {
      if (visible) {
        // 停留在会话里时每 10 秒拉一次，对方的新消息能自己冒出来
        this.timer = setInterval(() => this.load(true), 10000)
      } else {
        clearInterval(this.timer)
        this.timer = null
        this.content = ''
      }
    }
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    load(silent) {
      if (!dealChat.dealId) return
      if (!silent) this.loading = true
      axios.get('/api/deals/' + dealChat.dealId + '/messages').then(res => {
        if (res.data.code === 0) {
          this.info = res.data.data
          if (dealChat.onRead) dealChat.onRead()
        } else if (!silent) {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    send() {
      const text = (this.content || '').trim()
      if (!text) {
        this.$message.warning('消息不能为空')
        return
      }
      this.sending = true
      axios.post('/api/deals/' + dealChat.dealId + '/messages', { content: text }).then(res => {
        if (res.data.code === 0) {
          this.content = ''
          this.load(true)
        } else {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        this.sending = false
      })
    },
    statusText(status) {
      return { PENDING: '待成交', COMPLETED: '已完成', CANCELLED: '已取消' }[status] || ''
    },
    tagClass(status) {
      if (status === 'PENDING') return 'pending'
      if (status === 'COMPLETED') return 'onsale'
      return 'off'
    },
    formatTime(time) {
      if (!time) return ''
      return String(time).replace('T', ' ').slice(5, 16)
    }
  }
}
</script>

<style scoped>
.chat-item { display: flex; align-items: center; gap: 8px; font-size: 14px; }
.chat-item-title { font-weight: 600; }
.chat-hint { margin: 8px 0 0; color: var(--text-muted); font-size: 12px; line-height: 18px; }
.chat-body {
  height: 260px; overflow-y: auto; margin-top: 10px; padding: 10px 12px;
  background: #f7f9fc; border: 1px solid var(--border); border-radius: 6px;
}
.chat-row { display: flex; margin-bottom: 10px; }
.chat-row.mine { justify-content: flex-end; }
.chat-bubble {
  max-width: 78%; padding: 8px 10px; background: #fff; border: 1px solid var(--border);
  border-radius: 8px; font-size: 13px; line-height: 20px;
}
.chat-row.mine .chat-bubble { background: #e8f1ff; border-color: #c9ddff; }
.chat-text { white-space: pre-wrap; word-break: break-word; }
.chat-time { margin-top: 4px; color: var(--text-muted); font-size: 11px; }
</style>
