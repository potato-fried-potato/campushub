<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">举报处置</h1>
        <p class="page-sub">共 {{ total }} 条举报记录</p>
      </div>
    </div>
    <div class="panel">
      <div class="panel-body">
        <div class="toolbar" style="margin-bottom: 12px">
          <el-radio-group v-model="status" @change="search">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="PENDING">待处理</el-radio-button>
            <el-radio-button value="OFF_SHELF">已下架</el-radio-button>
            <el-radio-button value="REJECTED">已驳回</el-radio-button>
          </el-radio-group>
          <el-button text @click="load">刷新</el-button>
        </div>
        <el-table :data="reports" v-loading="loading" empty-text="暂无举报记录">
          <el-table-column label="被举报商品" min-width="180">
            <template #default="scope">
              <el-button link type="primary" @click="openItem(scope.row)">{{ scope.row.itemTitle }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="reporterNickname" label="举报人" width="110" />
          <el-table-column label="原因" width="110">
            <template #default="scope">{{ reasonText(scope.row.reason) }}</template>
          </el-table-column>
          <el-table-column prop="detail" label="补充说明" min-width="160" />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <span class="tag" :class="scope.row.status === 'PENDING' ? 'pending' : (scope.row.status === 'OFF_SHELF' ? 'sold' : 'off')">
                {{ reportStatus(scope.row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <template v-if="scope.row.status === 'PENDING'">
                <el-button link type="danger" @click="handle(scope.row, 'OFF_SHELF')">下架商品</el-button>
                <el-button link @click="handle(scope.row, 'REJECT')">驳回</el-button>
              </template>
              <span v-else style="color: var(--text-muted); font-size: 12px">已处置</span>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-if="total > pageSize"
          style="margin-top: 14px; justify-content: center"
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          @current-change="changePage"
        />
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'AdminReportsView',
  data() {
    return { status: '', reports: [], total: 0, pageNum: 1, pageSize: 20, loading: false }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      axios.get('/api/admin/reports', { params: { status: this.status, pageNum: this.pageNum, pageSize: this.pageSize } }).then(res => {
        if (res.data.code === 0) {
          this.reports = res.data.data.list
          this.total = res.data.data.total
        }
      }).finally(() => { this.loading = false })
    },
    search() {
      this.pageNum = 1
      this.load()
    },
    changePage(page) {
      this.pageNum = page
      this.load()
    },
    openItem(row) {
      window.open('/#/item/' + row.itemId, '_blank')
    },
    handle(row, action) {
      const text = action === 'OFF_SHELF' ? '下架该商品并结案' : '驳回该举报'
      this.$confirm('确定要' + text + '吗？', '提示').then(() => {
        axios.patch('/api/admin/reports/' + row.id, { action }).then(res => {
          if (res.data.code === 0) {
            this.$message.success('处置成功')
            this.load()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    reasonText(reason) {
      return { FAKE: '虚假信息', ILLEGAL: '违禁物品', SCAM: '诈骗风险', OTHER: '其他' }[reason] || reason
    },
    reportStatus(status) {
      return { PENDING: '待处理', OFF_SHELF: '已下架', REJECTED: '已驳回' }[status] || status
    }
  }
}
</script>
