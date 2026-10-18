<template>
  <div class="main-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的购买</h1>
        <p class="page-sub">下单后在这里联系卖家、确认成交并评价</p>
      </div>
      <div style="display: flex; gap: 10px">
        <el-button text @click="load">刷新</el-button>
        <el-button @click="$router.push('/')">去集市逛逛</el-button>
      </div>
    </div>

    <div class="empty-state panel" v-if="!user">
      <div class="empty-title">登录后才能查看购买记录</div>
      <el-button type="primary" @click="openAuth('login')">去登录</el-button>
    </div>

    <template v-else>
      <div class="stat-strip" style="grid-template-columns: repeat(4, 1fr)">
        <div class="stat-cell warning">
          <div class="value">{{ pendingCount }}</div>
          <div class="label">待成交</div>
        </div>
        <div class="stat-cell brand">
          <div class="value">{{ completedCount }}</div>
          <div class="label">已成交</div>
        </div>
        <div class="stat-cell">
          <div class="value">{{ reviewCount }}</div>
          <div class="label">待我评价</div>
        </div>
        <div class="stat-cell price">
          <div class="value">￥{{ paidAmount }}</div>
          <div class="label">累计支出</div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-body">
          <div class="toolbar" style="margin-bottom: 12px">
            <el-radio-group v-model="status">
              <el-radio-button value="ALL">全部 ({{ deals.length }})</el-radio-button>
              <el-radio-button value="PENDING">待成交 ({{ pendingCount }})</el-radio-button>
              <el-radio-button value="COMPLETED">已完成 ({{ completedCount }})</el-radio-button>
              <el-radio-button value="CANCELLED">已取消 ({{ cancelledCount }})</el-radio-button>
            </el-radio-group>
          </div>

          <el-table :data="filtered" v-loading="loading" empty-text="还没有购买记录">
            <el-table-column label="商品" min-width="240">
              <template #default="scope">
                <div style="display: flex; gap: 10px; align-items: center">
                  <img
                    v-if="scope.row.itemCover"
                    :src="scope.row.itemCover"
                    style="width: 56px; height: 42px; object-fit: cover; border-radius: 4px"
                  />
                  <div>
                    <el-button link type="primary" @click="$router.push('/item/' + scope.row.itemId)">
                      {{ scope.row.itemTitle }}
                    </el-button>
                    <div style="color: var(--text-muted); font-size: 12px">
                      {{ sourceText(scope.row.source) }} · {{ timeText(scope.row) }}
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="成交价" width="110">
              <template #default="scope">
                <span style="color: var(--price); font-weight: 600">￥{{ scope.row.dealPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sellerNickname" label="卖家" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <span class="tag" :class="tagClass(scope.row.status)">{{ statusText(scope.row.status) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280">
              <template #default="scope">
                <el-button v-if="scope.row.status === 'PENDING'" link type="primary" @click="contactSeller(scope.row)">
                  联系卖家
                </el-button>
                <el-button v-if="scope.row.status === 'PENDING'" link type="primary" @click="act(scope.row, 'complete')">
                  确认成交
                </el-button>
                <el-button v-if="scope.row.status === 'PENDING'" link type="danger" @click="act(scope.row, 'cancel')">
                  取消
                </el-button>
                <el-button
                  v-if="scope.row.status === 'COMPLETED' && !scope.row.reviewed"
                  link
                  type="warning"
                  @click="$router.push('/item/' + scope.row.itemId)"
                >去评价</el-button>
                <el-button v-if="scope.row.status === 'COMPLETED'" link @click="contactSeller(scope.row)">
                  联系卖家
                </el-button>
                <el-button link @click="$router.push('/item/' + scope.row.itemId)">查看商品</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="empty-state" v-if="!loading && deals.length === 0">
            <div class="empty-title">还没有买过东西</div>
            <div>在集市里看中的闲置可以直接立即购买，或先出价 / 预约</div>
            <el-button type="primary" style="margin-top: 10px" @click="$router.push('/')">去集市</el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 还在谈的出价 / 预约：买家同样需要能联系卖家 -->
    <div class="panel" style="margin-top: 14px" v-if="user">
      <div class="panel-body">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px">
          <h3 style="margin: 0; font-size: 15px">我在谈的（出价 / 预约）</h3>
          <span style="color: var(--text-muted); font-size: 12px">
            {{ bargains.length + reservations.length }} 条记录
          </span>
        </div>

        <el-table :data="bargains" empty-text="还没有发出过出价">
          <el-table-column label="商品" min-width="220">
            <template #default="scope">
              <el-button link type="primary" @click="$router.push('/item/' + scope.row.itemId)">
                {{ scope.row.itemTitle }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="出价 / 挂牌价" width="160">
            <template #default="scope">
              <span style="color: var(--price); font-weight: 600">￥{{ scope.row.offerPrice }}</span>
              <span style="color: var(--text-muted)"> / ￥{{ scope.row.itemPrice }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <span class="tag" :class="tagClass(scope.row.status)">{{ bargainStatus(scope.row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="scope">
              <el-button link type="primary" @click="goItemMessage(scope.row)">去商品页留言</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-table :data="reservations" empty-text="还没有发出过预约" style="margin-top: 10px">
          <el-table-column label="商品" min-width="220">
            <template #default="scope">
              <el-button link type="primary" @click="$router.push('/item/' + scope.row.itemId)">
                {{ scope.row.itemTitle }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="expectTime" label="期望时间" width="170" />
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <span class="tag" :class="tagClass(scope.row.status)">{{ reservationStatus(scope.row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="scope">
              <el-button link type="primary" @click="goItemMessage(scope.row)">去商品页留言</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { openAuth } from '../auth-dialog'
import { auth } from '../auth-store'
import { openDealChat } from '../deal-chat'

export default {
  name: 'MyPurchasesView',
  data() {
    return {
      deals: [],
      bargains: [],
      reservations: [],
      status: 'ALL',
      loading: false
    }
  },
  computed: {
    user() {
      return auth.user
    },
    filtered() {
      if (this.status === 'ALL') return this.deals
      return this.deals.filter(d => d.status === this.status)
    },
    pendingCount() {
      return this.deals.filter(d => d.status === 'PENDING').length
    },
    completedCount() {
      return this.deals.filter(d => d.status === 'COMPLETED').length
    },
    cancelledCount() {
      return this.deals.filter(d => d.status === 'CANCELLED').length
    },
    reviewCount() {
      return this.deals.filter(d => d.status === 'COMPLETED' && !d.reviewed).length
    },
    paidAmount() {
      const total = this.deals
        .filter(d => d.status === 'COMPLETED')
        .reduce((sum, d) => sum + Number(d.dealPrice || 0), 0)
      return total.toFixed(2)
    }
  },
  watch: {
    // 弹窗登录成功后自动加载购买记录
    user: {
      immediate: true,
      handler(u) {
        if (u) this.load()
      }
    }
  },
  methods: {
    openAuth,
    load() {
      if (!this.user) return
      this.loading = true
      axios.get('/api/deals', { params: { view: 'sent' } }).then(res => {
        if (res.data.code === 0) {
          this.deals = res.data.data.list
        } else {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        this.loading = false
      })
      axios.get('/api/bargains', { params: { view: 'sent' } }).then(res => {
        if (res.data.code === 0) this.bargains = res.data.data.list
      })
      axios.get('/api/reservations', { params: { view: 'sent' } }).then(res => {
        if (res.data.code === 0) this.reservations = res.data.data.list
      })
    },
    // 交易单走私密会话弹窗；还没成交的出价 / 预约只能去公开的商品问答区留言
    contactSeller(row) {
      openDealChat({ id: row.id, itemTitle: row.itemTitle, status: row.status })
    },
    goItemMessage(row) {
      this.$router.push('/item/' + row.itemId + '?contact=1')
    },
    act(row, action) {
      const text = action === 'complete' ? '确认成交（商品将记为已售并开启互评）' : '取消交易单（商品将回到在售）'
      this.$confirm('确定要' + text + '吗？', '提示', { confirmButtonText: action === 'complete' ? '确认成交' : '确定' })
        .then(() => {
          axios.post('/api/deals/' + row.id + '/' + action).then(res => {
            if (res.data.code === 0) {
              this.$message.success(action === 'complete' ? '已确认成交' : '交易单已取消')
              this.load()
            } else {
              this.$message.error(res.data.message)
            }
          })
        }).catch(() => {})
    },
    statusText(status) {
      return { PENDING: '待成交', COMPLETED: '已完成', CANCELLED: '已取消' }[status] || status
    },
    bargainStatus(status) {
      return { PENDING: '待处理', ACCEPTED: '已接受', REJECTED: '已拒绝', CANCELLED: '已取消' }[status] || status
    },
    reservationStatus(status) {
      return { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已拒绝', CANCELLED: '已取消', CLOSED: '已关闭' }[status] || status
    },
    tagClass(status) {
      if (status === 'PENDING') return 'pending'
      if (status === 'COMPLETED') return 'onsale'
      return 'off'
    },
    sourceText(source) {
      return { DIRECT: '立即购买', BARGAIN: '砍价成交', RESERVATION: '预约成交' }[source] || source
    },
    timeText(row) {
      const time = row.status === 'COMPLETED' ? row.completedAt : (row.status === 'CANCELLED' ? row.cancelledAt : row.createdAt)
      if (!time) return row.createdAt || ''
      return String(time).replace('T', ' ').slice(0, 16)
    }
  }
}
</script>
