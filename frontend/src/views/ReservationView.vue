<template>
  <div class="page-box">
    <div class="page-head">
      <div>
        <h1 class="page-title">意向预约</h1>
        <p class="page-sub">确认线下面交时间，确认后商品将被锁定</p>
      </div>
    </div>
    <el-radio-group v-model="view" @change="load">
      <el-radio-button value="received">我收到的</el-radio-button>
      <el-radio-button value="sent">我发出的</el-radio-button>
    </el-radio-group>
    <span class="pending">待处理 {{ pendingCount }} 条</span>

    <el-table :data="list" style="width: 100%; margin-top: 15px">
      <el-table-column prop="itemTitle" label="商品" />
      <el-table-column prop="buyerNickname" label="买家" width="100" />
      <el-table-column prop="expectTime" label="期望交易时间" width="180" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="状态" width="90">
        <template #default="scope">{{ statusText(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="scope">
          <template v-if="scope.row.status === 'PENDING'">
            <template v-if="view === 'received'">
              <el-button link type="primary" @click="handle(scope.row.id, 'confirm')">确认</el-button>
              <el-button link type="danger" @click="handle(scope.row.id, 'reject')">拒绝</el-button>
            </template>
            <el-button v-else link type="warning" @click="handle(scope.row.id, 'cancel')">取消</el-button>
          </template>
          <el-button
            v-if="view === 'sent' && scope.row.status === 'CONFIRMED'"
            link
            type="warning"
            @click="handle(scope.row.id, 'cancel')"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="empty" v-if="list.length === 0">暂无预约记录</div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'ReservationView',
  data() {
    return { view: 'received', list: [], pendingCount: 0 }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      axios.get('/api/reservations', { params: { view: this.view } }).then(res => {
        if (res.data.code === 0) {
          this.list = res.data.data.list
          this.pendingCount = res.data.data.pendingCount
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    handle(id, action) {
      this.$confirm('确定执行该操作吗？', '提示').then(() => {
        axios.post('/api/reservations/' + id + '/' + action).then(res => {
          if (res.data.code === 0) {
            this.$message.success('操作成功')
            this.load()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    statusText(status) {
      const map = {
        PENDING: '待确认',
        CONFIRMED: '已确认',
        REJECTED: '已拒绝',
        CANCELLED: '已取消',
        CLOSED: '已关闭'
      }
      return map[status] || status
    }
  }
}
</script>

<style scoped>
.page-box { padding: 20px 24px; }
.pending { margin-left: 12px; color: #e4393c; font-size: 13px; }
.empty { margin-top: 30px; color: #999; text-align: center; }
</style>
