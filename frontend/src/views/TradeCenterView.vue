<template>
  <div class="main-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">交易中心</h1>
        <p class="page-sub">交易单、砍价、预约与留言都在这里处理</p>
      </div>
      <el-radio-group v-model="view" @change="loadAll">
        <el-radio-button value="received">我收到的</el-radio-button>
        <el-radio-button value="sent">我发出的</el-radio-button>
      </el-radio-group>
    </div>

    <div class="stat-strip" style="grid-template-columns: repeat(4, 1fr)">
      <div class="stat-cell brand">
        <div class="value">{{ dealCount }}</div>
        <div class="label">待成交交易单</div>
      </div>
      <div class="stat-cell warning">
        <div class="value">{{ bargainCount }}</div>
        <div class="label">待处理出价</div>
      </div>
      <div class="stat-cell warning">
        <div class="value">{{ reservationCount }}</div>
        <div class="label">待处理预约</div>
      </div>
      <div class="stat-cell price">
        <div class="value">{{ totalUnread }}</div>
        <div class="label">未读留言</div>
      </div>
    </div>

    <el-tabs v-model="tab" class="panel" style="padding: 8px 16px 16px">
      <el-tab-pane :label="'交易单 (' + deals.length + ')'" name="deal">
        <el-table :data="deals" v-loading="loading" empty-text="暂无交易单">
          <el-table-column label="商品" min-width="180">
            <template #default="scope">
              <el-button link type="primary" @click="$router.push('/item/' + scope.row.itemId)">
                {{ scope.row.itemTitle }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="成交价" width="110">
            <template #default="scope">
              <span style="color: var(--price); font-weight: 600">￥{{ scope.row.dealPrice }}</span>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="100">
            <template #default="scope">
              <span class="tag" :class="sourceClass(scope.row.source)">{{ sourceText(scope.row.source) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="买家 / 卖家" min-width="160">
            <template #default="scope">
              {{ scope.row.buyerNickname }} / {{ scope.row.sellerNickname }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="scope">
              <span class="tag" :class="tagClass(scope.row.status)">{{ dealStatus(scope.row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="scope">
              <el-button v-if="scope.row.status !== 'CANCELLED'" link type="primary" @click="contactDealParty(scope.row)">
                {{ view === 'sent' ? '联系卖家' : '联系买家' }}
              </el-button>
              <template v-if="scope.row.status === 'PENDING'">
                <el-button link type="primary" @click="act('deals', scope.row.id, 'complete')">确认成交</el-button>
                <el-button link type="danger" @click="act('deals', scope.row.id, 'cancel')">取消</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="page-sub" style="margin-top: 10px">线下当面交易完成后再点「确认成交」。</div>
      </el-tab-pane>

      <el-tab-pane :label="'砍价 (' + bargains.length + ')'" name="bargain">
        <el-table :data="bargains" v-loading="loading" empty-text="暂无砍价记录">
          <el-table-column label="商品" min-width="180">
            <template #default="scope">
              <el-button link type="primary" @click="$router.push('/item/' + scope.row.itemId)">
                {{ scope.row.itemTitle }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="出价 / 挂牌价" width="150">
            <template #default="scope">
              <span style="color: var(--price); font-weight: 600">￥{{ scope.row.offerPrice }}</span>
              <span style="color: var(--text-muted)"> / ￥{{ scope.row.itemPrice }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="留言" min-width="140" />
          <el-table-column prop="buyerNickname" label="买家" width="100" />
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <span class="tag" :class="tagClass(scope.row.status)">{{ bargainStatus(scope.row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <template v-if="scope.row.status === 'PENDING'">
                <template v-if="view === 'received'">
                  <el-button link type="primary" @click="act('bargains', scope.row.id, 'accept')">接受</el-button>
                  <el-button link type="danger" @click="act('bargains', scope.row.id, 'reject')">拒绝</el-button>
                </template>
                <el-button v-else link type="warning" @click="act('bargains', scope.row.id, 'cancel')">取消</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="'预约 (' + reservations.length + ')'" name="reservation">
        <el-table :data="reservations" v-loading="loading" empty-text="暂无预约记录">
          <el-table-column label="商品" min-width="180">
            <template #default="scope">
              <el-button link type="primary" @click="$router.push('/item/' + scope.row.itemId)">
                {{ scope.row.itemTitle }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="buyerNickname" label="买家" width="100" />
          <el-table-column prop="expectTime" label="期望时间" width="170" />
          <el-table-column prop="remark" label="备注" min-width="140" />
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <span class="tag" :class="tagClass(scope.row.status)">{{ reservationStatus(scope.row.status) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <template v-if="view === 'received' && scope.row.status === 'PENDING'">
                <el-button link type="primary" @click="act('reservations', scope.row.id, 'confirm')">确认</el-button>
                <el-button link type="danger" @click="act('reservations', scope.row.id, 'reject')">拒绝</el-button>
              </template>
              <el-button
                v-if="view === 'sent' && (scope.row.status === 'PENDING' || scope.row.status === 'CONFIRMED')"
                link
                type="warning"
                @click="act('reservations', scope.row.id, 'cancel')"
              >取消</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="'留言 (' + messages.length + ')'" name="message">
        <div v-for="m in messages" :key="m.itemId" class="msg-item">
          <div class="msg-head">
            <span class="msg-author">{{ m.peerNickname }}</span>
            <span>{{ m.lastTime }}</span>
          </div>
          <div style="margin-top: 2px">{{ m.lastContent }}</div>
          <div style="margin-top: 6px; display: flex; align-items: center; gap: 10px">
            <el-button link type="primary" @click="$router.push('/item/' + m.itemId)">
              进入「{{ m.itemTitle }}」
            </el-button>
            <span class="tag pending" v-if="m.unreadCount > 0">{{ m.unreadCount }} 条未读</span>
            <el-button v-if="m.unreadCount > 0" link type="warning" @click="markRead(m.itemId)">标记已读</el-button>
          </div>
        </div>
        <div class="empty-state" v-if="messages.length === 0">
          <div class="empty-title">还没有留言往来</div>
          <div>在商品详情页向卖家提问后，这里会显示对话</div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import axios from 'axios'
import { openDealChat } from '../deal-chat'

export default {
  name: 'TradeCenterView',
  data() {
    return {
      tab: 'bargain',
      view: 'received',
      loading: false,
      bargains: [],
      bargainCount: 0,
      deals: [],
      dealCount: 0,
      reservations: [],
      reservationCount: 0,
      messages: [],
      totalUnread: 0,
      timer: null
    }
  },
  created() {
    this.loadAll()
  },
  mounted() {
    // 停留期间每 30 秒刷新一次，出价/预约/留言状态变化能及时看到
    this.timer = setInterval(() => this.loadAll(), 30000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    loadAll() {
      this.loading = true
      const done = () => { this.loading = false }
      axios.get('/api/bargains', { params: { view: this.view } }).then(res => {
        if (res.data.code === 0) {
          this.bargains = res.data.data.list
          this.bargainCount = res.data.data.pendingCount
        }
      }).finally(done)
      axios.get('/api/deals', { params: { view: this.view } }).then(res => {
        if (res.data.code === 0) {
          this.deals = res.data.data.list
          this.dealCount = res.data.data.pendingCount
        }
      })
      axios.get('/api/reservations', { params: { view: this.view } }).then(res => {
        if (res.data.code === 0) {
          this.reservations = res.data.data.list
          this.reservationCount = res.data.data.pendingCount
        }
      })
      axios.get('/api/messages/mine', { params: { view: this.view } }).then(res => {
        if (res.data.code === 0) {
          this.messages = res.data.data.list
          this.totalUnread = res.data.data.totalUnread
        }
      })
    },
    act(resource, id, action) {
      const text = {
        'bargains:accept': '接受该出价（商品将被锁定为待成交）',
        'bargains:reject': '拒绝该出价',
        'bargains:cancel': '取消该出价',
        'reservations:confirm': '确认该预约（商品将锁定给这位买家）',
        'reservations:reject': '拒绝该预约',
        'reservations:cancel': '取消该预约',
        'deals:complete': '确认成交（商品将记为已售并开启互评）',
        'deals:cancel': '取消交易单（商品将回到在售）'
      }[resource + ':' + action] || '执行该操作'
      const okText = action === 'complete' ? '确认成交' : '确定'
      this.$confirm('确定要' + text + '吗？', '提示', { confirmButtonText: okText }).then(() => {
        axios.post('/api/' + resource + '/' + id + '/' + action).then(res => {
          if (res.data.code === 0) {
            this.$message.success('操作成功')
            this.loadAll()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    markRead(itemId) {
      axios.patch('/api/messages/item/' + itemId + '/read').then(res => {
        if (res.data.code === 0) {
          this.$message.success('已标记为已读')
          this.loadAll()
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    // 交易单私密会话：只有买卖双方可见，和公开的商品问答不是一回事
    contactDealParty(row) {
      openDealChat(row)
    },
    tagClass(status) {
      if (status === 'PENDING') return 'pending'
      if (status === 'ACCEPTED' || status === 'CONFIRMED') return 'onsale'
      if (status === 'REJECTED' || status === 'CANCELLED' || status === 'CLOSED') return 'off'
      return ''
    },
    bargainStatus(status) {
      return { PENDING: '待处理', ACCEPTED: '已接受', REJECTED: '已拒绝', CANCELLED: '已取消' }[status] || status
    },
    reservationStatus(status) {
      return { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已拒绝', CANCELLED: '已取消', CLOSED: '已关闭' }[status] || status
    },
    dealStatus(status) {
      return { PENDING: '待成交', COMPLETED: '已完成', CANCELLED: '已取消' }[status] || status
    },
    sourceText(source) {
      return { DIRECT: '立即购买', BARGAIN: '砍价成交', RESERVATION: '预约成交' }[source] || source
    },
    sourceClass(source) {
      return source === 'DIRECT' ? 'onsale' : (source === 'BARGAIN' ? 'pending' : '')
    }
  }
}
</script>
