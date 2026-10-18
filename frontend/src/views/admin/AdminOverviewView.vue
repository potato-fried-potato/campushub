<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">数据概览</h1>
        <p class="page-sub">平台运行状态与待处理事项</p>
      </div>
      <el-button text @click="load">刷新</el-button>
    </div>

    <div class="stat-strip">
      <div class="stat-cell brand"><div class="value">{{ stats.onSaleCount }}</div><div class="label">在售商品</div></div>
      <div class="stat-cell price"><div class="value">{{ stats.soldCount }}</div><div class="label">累计成交</div></div>
      <div class="stat-cell success"><div class="value">{{ stats.certifiedUserCount }}</div><div class="label">认证用户</div></div>
      <div class="stat-cell warning"><div class="value">{{ stats.totalViewCount }}</div><div class="label">累计浏览</div></div>
    </div>

    <div class="stat-strip" style="grid-template-columns: repeat(3, 1fr)">
      <div class="stat-cell">
        <div class="value">{{ userCount }}</div>
        <div class="label">注册用户</div>
      </div>
      <div class="stat-cell">
        <div class="value">{{ pendingReports }}</div>
        <div class="label">待处理举报</div>
      </div>
      <div class="stat-cell">
        <div class="value">{{ offShelfCount }}</div>
        <div class="label">已下架商品</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-body">
        <h3 style="margin: 0 0 8px; font-size: 15px">快捷入口</h3>
        <div style="display: flex; gap: 10px; flex-wrap: wrap">
          <el-button @click="go('users')">处理用户</el-button>
          <el-button @click="go('items')">审核商品</el-button>
          <el-button type="primary" @click="go('reports')">处置举报</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminOverviewView',
  data() {
    return {
      stats: { onSaleCount: 0, soldCount: 0, certifiedUserCount: 0, totalViewCount: 0 },
      userCount: 0,
      pendingReports: 0,
      offShelfCount: 0
    }
  },
  created() {
    this.load()
  },
  methods: {
    go(page) {
      this.$router.push('/admin/' + page)
    },
    load() {
      axios.get('/api/system/stats').then(res => {
        if (res.data.code === 0) this.stats = res.data.data
      })
      axios.get('/api/admin/users', { params: { pageSize: 1 } }).then(res => {
        if (res.data.code === 0) this.userCount = res.data.data.total
      })
      axios.get('/api/admin/reports', { params: { status: 'PENDING', pageSize: 1 } }).then(res => {
        if (res.data.code === 0) this.pendingReports = res.data.data.total
      })
      axios.get('/api/admin/items', { params: { status: 'OFF_SHELF', pageSize: 1 } }).then(res => {
        if (res.data.code === 0) this.offShelfCount = res.data.data.total
      })
    }
  }
}
</script>
