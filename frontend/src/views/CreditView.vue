<template>
  <div class="main-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的信用</h1>
        <p class="page-sub">信用分由基础分与交易评价共同决定</p>
      </div>
      <el-button text @click="load">刷新</el-button>
    </div>

    <div class="panel">
      <div class="panel-body">
        <div class="credit-row">
          <div class="score">{{ received.creditScore }}</div>
          <div>
            <div class="level">信用{{ received.creditLevel }}</div>
            <div class="sub">
              基础分 {{ received.baseScore }} · 评价调整 {{ received.ratingDelta >= 0 ? '+' : '' }}{{ received.ratingDelta }} ·
              平均 {{ received.avgRating }} 星（{{ received.reviewCount }} 条评价）
            </div>
          </div>
        </div>
        <div class="rule">
          计分规则：基础分 80；每条评价按星级加减（5★ +10 / 4★ +5 / 3★ 0 / 2★ -5 / 1★ -10），最终限制在 0~100 分。
        </div>
      </div>
    </div>

    <el-tabs v-model="tab" class="panel" style="padding: 8px 16px 16px; margin-top: 14px">
      <el-tab-pane :label="'收到的评价 (' + received.list.length + ')'" name="received">
        <el-table :data="received.list" empty-text="还没有收到评价">
          <el-table-column prop="reviewerNickname" label="评价人" width="120" />
          <el-table-column label="评分" width="90">
            <template #default="scope"><span class="tag sold">{{ scope.row.rating }} 星</span></template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="200" />
          <el-table-column prop="createdAt" label="时间" width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="'我发出的评价 (' + mine.list.length + ')'" name="sent">
        <el-table :data="mine.list" empty-text="还没有发出过评价">
          <el-table-column prop="itemTitle" label="商品" min-width="180" />
          <el-table-column prop="targetNickname" label="被评价人" width="120" />
          <el-table-column label="评分" width="90">
            <template #default="scope"><span class="tag onsale">{{ scope.row.rating }} 星</span></template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="180" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="'待我评价 (' + mine.pending.length + ')'" name="pending">
        <div v-for="p in mine.pending" :key="p.itemId" class="pending-row">
          <span>{{ p.itemTitle }}</span>
          <el-button link type="primary" @click="$router.push('/item/' + p.itemId)">去评价</el-button>
        </div>
        <div class="empty-state" v-if="mine.pending.length === 0">
          <div class="empty-title">没有待评价的成交</div>
          <div>成交后可以在这里补评价</div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="'我的举报 (' + reports.length + ')'" name="reports">
        <el-table :data="reports" empty-text="还没有提交过举报">
          <el-table-column prop="itemTitle" label="被举报商品" min-width="180" />
          <el-table-column label="原因" width="110">
            <template #default="scope">{{ reasonText(scope.row.reason) }}</template>
          </el-table-column>
          <el-table-column prop="detail" label="补充说明" min-width="160" />
          <el-table-column label="处理结果" width="110">
            <template #default="scope">
              <span class="tag" :class="scope.row.status === 'PENDING' ? 'pending' : (scope.row.status === 'OFF_SHELF' ? 'sold' : 'off')">
                {{ reportStatus(scope.row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="提交时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import axios from 'axios'
import { auth } from '../auth-store'

export default {
  name: 'CreditView',
  data() {
    return {
      tab: 'received',
      received: { avgRating: 0, reviewCount: 0, list: [], baseScore: 80, ratingDelta: 0, creditScore: 80, creditLevel: '良好' },
      mine: { list: [], pending: [] },
      reports: []
    }
  },
  computed: {
    user() {
      return auth.user
    }
  },
  watch: {
    user: {
      immediate: true,
      handler(u) {
        if (u) this.load()
      }
    }
  },
  methods: {
    load() {
      if (!this.user) return
      axios.get('/api/users/' + this.user.id + '/reviews').then(res => {
        if (res.data.code === 0) this.received = res.data.data
      })
      axios.get('/api/reviews/mine').then(res => {
        if (res.data.code === 0) this.mine = res.data.data
      })
      axios.get('/api/reports/mine').then(res => {
        if (res.data.code === 0) this.reports = res.data.data
      })
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

<style scoped>
.credit-row { display: flex; align-items: center; gap: 16px; }
.score { color: var(--price); font-size: 40px; font-weight: 800; line-height: 1; }
.level { font-size: 15px; font-weight: 700; }
.sub { margin-top: 4px; color: var(--text-muted); font-size: 13px; }
.rule { margin-top: 12px; padding-top: 10px; border-top: 1px dashed var(--border); color: var(--text-muted); font-size: 12px; line-height: 20px; }
.pending-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 0; border-bottom: 1px solid #f1f4f9;
}
.pending-row:last-child { border-bottom: 0; }
</style>
