<template>
  <div class="page-box">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的留言</h1>
        <p class="page-sub">查看商品问答的来往记录</p>
      </div>
    </div>
    <el-radio-group v-model="view" @change="load">
      <el-radio-button value="received">我收到的</el-radio-button>
      <el-radio-button value="sent">我发出的</el-radio-button>
    </el-radio-group>
    <span class="unread" v-if="view === 'received'">未读 {{ totalUnread }} 条</span>

    <div class="list">
      <div class="card" v-for="m in list" :key="m.itemId">
        <div class="row">
          <span class="title">{{ m.itemTitle }}</span>
          <span class="badge" v-if="m.unreadCount > 0">{{ m.unreadCount }} 条未读</span>
        </div>
        <div class="content">{{ m.peerNickname }}：{{ m.lastContent }}</div>
        <div class="time">{{ m.lastTime }}</div>
        <el-button link type="primary" @click="$router.push('/item/' + m.itemId)">进入商品</el-button>
      </div>
    </div>
    <div class="empty" v-if="list.length === 0">暂无留言</div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'MessagesView',
  data() {
    return { view: 'received', list: [], totalUnread: 0 }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      axios.get('/api/messages/mine', { params: { view: this.view } }).then(res => {
        if (res.data.code === 0) {
          this.list = res.data.data.list
          this.totalUnread = res.data.data.totalUnread
        } else {
          this.$message.error(res.data.message)
        }
      })
    }
  }
}
</script>

<style scoped>
.page-box {
  padding: 20px 24px;
}

.unread {
  margin-left: 12px;
  color: #e4393c;
  font-size: 13px;
}

.list {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card {
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-weight: bold;
}

.badge {
  padding: 2px 8px;
  background: #fdf0f0;
  border-radius: 4px;
  color: #e4393c;
  font-size: 12px;
}

.content {
  margin-top: 6px;
  color: #555;
  font-size: 14px;
}

.time {
  margin-top: 4px;
  color: #999;
  font-size: 12px;
}

.empty {
  margin-top: 30px;
  color: #999;
  text-align: center;
}
</style>
