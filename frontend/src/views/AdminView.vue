<template>
  <div class="page-box">
    <div class="page-head">
      <div>
        <h1 class="page-title">管理后台</h1>
        <p class="page-sub">用户、商品与举报的统一处置入口</p>
      </div>
    </div>
    <div class="stat-box">
      <div class="stat-item"><div class="num">{{ stats.onSaleCount }}</div><div class="label">在售</div></div>
      <div class="stat-item"><div class="num">{{ stats.soldCount }}</div><div class="label">成交</div></div>
      <div class="stat-item"><div class="num">{{ stats.certifiedUserCount }}</div><div class="label">认证用户</div></div>
      <div class="stat-item"><div class="num">{{ stats.totalViewCount }}</div><div class="label">累计浏览</div></div>
    </div>

    <el-tabs v-model="tab" @tab-change="loadTab">
      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users">
          <el-table-column prop="studentNo" label="学号" width="150" />
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column prop="phone" label="电话" width="140" />
          <el-table-column prop="role" label="角色" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="scope">{{ scope.row.status === 'ACTIVE' ? '正常' : '已禁用' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                link
                :type="scope.row.status === 'ACTIVE' ? 'danger' : 'primary'"
                :disabled="scope.row.role === 'ADMIN'"
                @click="toggleUser(scope.row)"
              >{{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="商品管理" name="items">
        <el-radio-group v-model="itemStatus" @change="loadItems">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="ON_SALE">在售</el-radio-button>
          <el-radio-button value="OFF_SHELF">已下架</el-radio-button>
          <el-radio-button value="SOLD">已售</el-radio-button>
        </el-radio-group>
        <el-table :data="items" style="margin-top: 12px">
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="price" label="价格" width="90" />
          <el-table-column prop="sellerName" label="卖家" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="scope">{{ statusText(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="scope">
              <el-button
                v-if="scope.row.status !== 'OFF_SHELF'"
                link
                type="danger"
                @click="offShelf(scope.row)"
              >强制下架</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="举报处置" name="reports">
        <el-table :data="reports">
          <el-table-column prop="itemTitle" label="商品" />
          <el-table-column prop="reporterNickname" label="举报人" width="110" />
          <el-table-column prop="reason" label="原因" width="100" />
          <el-table-column prop="detail" label="说明" />
          <el-table-column label="状态" width="110">
            <template #default="scope">{{ reportStatus(scope.row.status) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170">
            <template #default="scope">
              <template v-if="scope.row.status === 'PENDING'">
                <el-button link type="danger" @click="handleReport(scope.row, 'OFF_SHELF')">下架</el-button>
                <el-button link @click="handleReport(scope.row, 'REJECT')">驳回</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminView',
  data() {
    return {
      tab: 'users',
      stats: { onSaleCount: 0, soldCount: 0, certifiedUserCount: 0, totalViewCount: 0 },
      users: [],
      items: [],
      reports: [],
      itemStatus: ''
    }
  },
  created() {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (!user || user.role !== 'ADMIN') {
      this.$message.warning('仅管理员可访问')
      this.$router.push('/')
      return
    }
    axios.get('/api/system/stats').then(res => {
      if (res.data.code === 0) {
        this.stats = res.data.data
      }
    })
    this.loadTab('users')
  },
  methods: {
    loadTab(name) {
      if (name === 'users') this.loadUsers()
      if (name === 'items') this.loadItems()
      if (name === 'reports') this.loadReports()
    },
    loadUsers() {
      axios.get('/api/admin/users', { params: { pageSize: 100 } }).then(res => {
        if (res.data.code === 0) this.users = res.data.data.list
      })
    },
    loadItems() {
      axios.get('/api/admin/items', { params: { status: this.itemStatus, pageSize: 100 } }).then(res => {
        if (res.data.code === 0) this.items = res.data.data.list
      })
    },
    loadReports() {
      axios.get('/api/admin/reports', { params: { pageSize: 100 } }).then(res => {
        if (res.data.code === 0) this.reports = res.data.data.list
      })
    },
    toggleUser(row) {
      const status = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
      axios.patch('/api/admin/users/' + row.id + '/status', { status }).then(res => {
        if (res.data.code === 0) {
          this.$message.success('操作成功')
          this.loadUsers()
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    offShelf(row) {
      this.$confirm('确定强制下架「' + row.title + '」吗？', '提示').then(() => {
        axios.patch('/api/admin/items/' + row.id + '/off-shelf').then(res => {
          if (res.data.code === 0) {
            this.$message.success('已下架')
            this.loadItems()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    handleReport(row, action) {
      axios.patch('/api/admin/reports/' + row.id, { action }).then(res => {
        if (res.data.code === 0) {
          this.$message.success('处置成功')
          this.loadReports()
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    statusText(status) {
      const map = { ON_SALE: '在售', OFF_SHELF: '已下架', SOLD: '已售' }
      return map[status] || status
    },
    reportStatus(status) {
      const map = { PENDING: '待处理', OFF_SHELF: '已下架', REJECTED: '已驳回' }
      return map[status] || status
    }
  }
}
</script>

<style scoped>
.page-box { padding: 20px 24px; }
.stat-box { display: flex; gap: 12px; margin-bottom: 15px; }
.stat-item {
  flex: 1;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}
.stat-item .num { color: #409eff; font-size: 22px; font-weight: bold; }
.stat-item .label { color: #888; font-size: 13px; }
</style>
