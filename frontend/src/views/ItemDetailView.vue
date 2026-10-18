<template>
  <div v-if="item" class="main-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">{{ item.title }}</h1>
        <p class="page-sub">
          {{ item.categoryName }} · {{ campusText(item.campus) }} · {{ conditionText(item.conditionLevel) }}
          <span class="tag" :class="item.status === 'ON_SALE' ? 'onsale' : (item.status === 'SOLD' ? 'sold' : 'off')" style="margin-left: 8px">
            {{ statusText(item.status) }}
          </span>
          <span class="tag pending" v-if="item.reserved" style="margin-left: 6px">待成交</span>
        </p>
      </div>
      <el-button text @click="$router.back()">返回集市</el-button>
    </div>

    <div class="detail-layout">
      <div class="panel">
        <div class="panel-body">
          <img class="gallery-main" :src="currentImage" :alt="item.title" />
          <div class="gallery-thumbs" v-if="item.images.length > 1">
            <img
              v-for="(img, index) in item.images"
              :key="index"
              :class="{ active: index === imageIndex }"
              :src="img"
              @click="imageIndex = index"
            />
          </div>
          <div style="margin-top: 14px; color: var(--text-sub); font-size: 14px; line-height: 24px">
            {{ item.description || '卖家没有填写描述' }}
          </div>
        </div>
      </div>

      <div>
        <div class="panel">
          <div class="panel-body">
            <div class="detail-price">
              <span style="font-size: 16px">￥</span>{{ item.price }}
              <span class="origin" v-if="item.originalPrice">￥{{ item.originalPrice }}</span>
              <span class="tag sold" v-if="saveAmount > 0" style="margin-left: 10px">立省 ￥{{ saveAmount }}</span>
            </div>

            <div class="detail-meta">
              <div>成色：{{ conditionText(item.conditionLevel) }}</div>
              <div>校区：{{ campusText(item.campus) }}</div>
              <div>分类：{{ item.categoryName }}</div>
              <div>浏览：{{ item.viewCount }} 次</div>
            </div>

            <!-- 交易聚合区：卖家信息 + 主要动作集中在一处 -->
            <div class="seller-card">
              <div style="display: flex; justify-content: space-between; align-items: center">
                <div>
                  <div style="font-weight: 600">{{ item.seller.nickname }}</div>
                  <div style="color: var(--text-muted); font-size: 12px">
                    学号认证 {{ maskStudentNo(item.seller.studentNo) }} · 信用 {{ item.seller.creditScore }} 分 · {{ item.seller.avgRating }} 星（{{ item.seller.reviewCount }} 条）
                  </div>
                </div>
                <div style="display: flex; gap: 6px">
                  <el-button size="small" @click="openSellerReviews">查看评价</el-button>
                  <el-button v-if="!item.isSeller && !isAdminUser" size="small" @click="contactSeller">联系卖家</el-button>
                </div>
              </div>
            </div>

            <!-- 交易单锁定提示：待成交期间商品不能再被出价/预约，成交靠双方线下确认 -->
            <div
              v-if="item.dealId && item.dealStatus === 'PENDING'"
              style="margin-top: 12px; padding: 12px; border: 1px solid #f0c36d; background: #fdf6e3; border-radius: 6px"
            >
              <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap">
                <div>
                  <div style="font-weight: 600; font-size: 14px">
                    {{ item.isSeller || item.isBuyer ? '待成交交易单' : '该商品已被其他同学锁定' }}
                  </div>
                  <div style="color: var(--text-muted); font-size: 12px; margin-top: 2px">
                    成交价 ￥{{ item.dealPrice || item.price }} · 线下当面交易后点「确认成交」，商品才会记为已售
                  </div>
                </div>
                <div v-if="item.isSeller || item.isBuyer" style="display: flex; gap: 8px">
                  <el-button size="small" @click="contactCounterpart">
                    {{ item.isSeller ? '联系买家' : '联系卖家' }}
                  </el-button>
                  <el-button type="primary" size="small" @click="dealAct('complete')">确认成交</el-button>
                  <el-button size="small" @click="dealAct('cancel')">取消交易单</el-button>
                </div>
              </div>
            </div>

            <!-- 成交回执：交易单完成后买家/卖家看到同一份成交记录 -->
            <div
              v-else-if="item.dealId && item.dealStatus === 'COMPLETED'"
              style="margin-top: 12px; padding: 12px; border: 1px solid #c8e6c9; background: #f1f8e9; border-radius: 6px"
            >
              <div style="font-weight: 600; font-size: 14px">交易已完成</div>
              <div style="color: var(--text-muted); font-size: 12px; margin-top: 2px">
                成交价 ￥{{ item.dealPrice || item.price }} · {{ dealSourceText(item.dealSource) }}
                <span v-if="item.canReview"> · 评价入口在下方，双方各可评价一次</span>
                <span v-else-if="item.isSeller || item.isBuyer"> · 这笔交易你已评价过</span>
              </div>
              <div v-if="item.isSeller || item.isBuyer" style="margin-top: 8px">
                <el-button size="small" @click="contactCounterpart">
                  {{ item.isSeller ? '联系买家' : '联系卖家' }}
                </el-button>
              </div>
            </div>

            <div v-if="!user" style="margin-top: 14px">
              <el-button type="primary" style="width: 100%" @click="openAuth('login')">登录后购买 / 出价 / 预约</el-button>
            </div>
            <div v-else-if="isAdminUser" style="margin-top: 14px; color: var(--text-muted); font-size: 13px">
              管理员账号仅用于后台管理，不参与用户交易
            </div>
            <div v-else-if="item.isSeller" style="margin-top: 14px">
              <div style="color: var(--text-muted); font-size: 13px; margin-bottom: 8px">这是你发布的商品</div>
              <div style="display: flex; gap: 10px; flex-wrap: wrap">
                <el-button v-if="item.status === 'ON_SALE' && !item.reserved" @click="ownerStatus('OFF_SHELF')">下架</el-button>
                <el-tooltip
                  v-if="item.status === 'ON_SALE' && !item.reserved"
                  content="不产生交易记录，双方无法互相评价"
                  placement="top"
                >
                  <el-button type="warning" @click="ownerStatus('SOLD')">线下已售</el-button>
                </el-tooltip>
                <el-button v-if="item.status === 'OFF_SHELF'" type="primary" @click="ownerStatus('ON_SALE')">重新上架</el-button>
                <el-button link @click="$router.push('/mine')">去我的发布</el-button>
              </div>
            </div>
            <div v-else style="margin-top: 14px">
              <div style="display: flex; gap: 10px; flex-wrap: wrap">
                <el-button v-if="item.canDirectBuy" type="primary" @click="directBuy">立即购买 ￥{{ item.price }}</el-button>
                <el-button v-if="item.canDirectBuy" @click="bargainVisible = true">我要出价</el-button>
                <el-button v-if="item.canDirectBuy" @click="reservationVisible = true">预约购买</el-button>
                <el-button v-if="item.canReview" type="primary" @click="reviewVisible = true">评价这笔交易</el-button>
              </div>
              <div style="margin-top: 8px" v-if="item.canReport">
                <el-button link type="danger" @click="reportVisible = true">举报该商品</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-body">
            <h3 style="margin: 0 0 8px; font-size: 15px">交易安全提示</h3>
            <div style="color: var(--text-sub); font-size: 13px; line-height: 22px">
              请在校内公共场所当面验货交易，确认无误后再付款；不要提前转账，也不要点击对方发来的陌生链接。
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 留言聚合区 -->
    <div class="panel" id="qa" style="margin-top: 14px">
      <div class="panel-body">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px">
          <h3 style="margin: 0; font-size: 15px">商品问答</h3>
          <span style="color: var(--text-muted); font-size: 12px">{{ messages.length }} 个话题</span>
        </div>

        <div v-for="t in messages" :key="t.id" class="msg-item">
          <div class="msg-head">
            <span class="msg-author">{{ t.sender.nickname }}</span>
            <span>{{ t.createdAt }}</span>
          </div>
          <div style="margin-top: 2px">{{ t.content }}</div>
          <div v-for="r in t.replies" :key="r.id" class="msg-reply">
            <span class="msg-author">{{ r.sender.nickname }}</span>：{{ r.content }}
          </div>
          <el-button v-if="user && !isOwner && !isAdminUser" link type="primary" style="margin-top: 6px" @click="replyTo = t.id">
            回复
          </el-button>
        </div>

        <div class="empty-state" v-if="messages.length === 0">
          <div class="empty-title">还没有人提问</div>
          <div>对商品有疑问可以先留言问卖家</div>
        </div>

        <div v-if="user && !isOwner && !isAdminUser" style="display: flex; gap: 10px; margin-top: 12px">
          <el-input ref="messageInput" v-model="messageContent" :placeholder="replyTo ? '回复该话题' : '向卖家提问，例如：还在吗？可以面交吗？'" maxlength="200" />
          <el-button type="primary" @click="postMessage">发送</el-button>
          <el-button v-if="replyTo" @click="replyTo = null">取消回复</el-button>
        </div>
        <div v-else-if="!user" class="empty-state" style="padding: 16px 0">
          <el-button link type="primary" @click="openAuth('login')">登录后可提问</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="bargainVisible" title="我要出价" width="420px" align-center>
      <div style="color: var(--text-sub); font-size: 13px; margin-bottom: 10px">
        挂牌价 ￥{{ item.price }}，可出价 ￥{{ item.bargainFloor }} ~ ￥{{ belowPriceText }}（最低 8 折）
      </div>
      <div style="display: flex; gap: 8px; margin-bottom: 10px">
        <el-button size="small" @click="fillBargain(0.9)">9 折 ￥{{ discountPrice(0.9) }}</el-button>
        <el-button size="small" @click="fillBargain(0.85)">8.5 折 ￥{{ discountPrice(0.85) }}</el-button>
        <el-button size="small" @click="fillBargain(0.8)">8 折 ￥{{ discountPrice(0.8) }}</el-button>
      </div>
      <el-input v-model="bargainPrice" placeholder="你的出价金额" style="margin-bottom: 10px" />
      <el-input v-model="bargainMessage" placeholder="留言（选填）" />
      <div style="margin-top: 20px; display: flex; gap: 10px">
        <el-button type="primary" @click="submitBargain">提交出价</el-button>
        <el-button @click="bargainVisible = false">取消</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="reservationVisible" title="预约购买" width="440px" align-center>
      <div style="color: var(--text-sub); font-size: 13px; line-height: 20px; margin-bottom: 10px">
        挂牌价 ￥{{ item.price }}。提交后由卖家确认，确认即按此价为你锁定商品，再约时间当面验货付款；卖家确认前商品仍可能被别人买走。
      </div>
      <el-input v-model="expectTime" placeholder="期望面交时间，如 2026-11-01T15:00:00（选填）" style="margin-bottom: 10px" />
      <el-input v-model="reservationRemark" placeholder="备注，如：东风校区图书馆门口（选填）" />
      <div style="margin-top: 20px; display: flex; gap: 10px">
        <el-button type="primary" @click="submitReservation">提交预约</el-button>
        <el-button @click="reservationVisible = false">取消</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="举报商品" width="420px" align-center>
      <el-select v-model="reportReason" style="width: 100%; margin-bottom: 10px">
        <el-option label="虚假信息" value="FAKE" />
        <el-option label="违禁物品" value="ILLEGAL" />
        <el-option label="诈骗风险" value="SCAM" />
        <el-option label="其他" value="OTHER" />
      </el-select>
      <el-input v-model="reportDetail" type="textarea" :rows="3" placeholder="补充说明（选填）" />
      <div style="margin-top: 20px; display: flex; gap: 10px">
        <el-button type="danger" @click="submitReport">提交举报</el-button>
        <el-button @click="reportVisible = false">取消</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="评价交易" width="420px" align-center>
      <el-rate v-model="reviewRating" style="margin-bottom: 10px" />
      <el-input v-model="reviewContent" type="textarea" :rows="3" placeholder="说说这次交易体验（选填）" />
      <div style="margin-top: 20px; display: flex; gap: 10px">
        <el-button type="primary" @click="submitReview">提交评价</el-button>
        <el-button @click="reviewVisible = false">取消</el-button>
      </div>
    </el-dialog>

    <el-dialog v-model="sellerReviewsVisible" :title="(item.seller.nickname || '卖家') + ' 的信用评价'" width="520px">
      <div style="display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px">
        <span style="color: var(--price); font-size: 26px; font-weight: 700">{{ sellerReviews.avgRating }}</span>
        <span style="color: var(--text-muted); font-size: 13px">平均评分 · 共 {{ sellerReviews.reviewCount }} 条</span>
      </div>
      <div v-for="r in sellerReviews.list" :key="r.id" class="msg-item">
        <div class="msg-head">
          <span class="msg-author">{{ r.reviewerNickname }}</span>
          <span class="tag sold">{{ r.rating }} 星</span>
        </div>
        <div style="margin-top: 4px">{{ r.content || '（未填写评价内容）' }}</div>
        <div class="time">{{ r.createdAt }}</div>
      </div>
      <div class="empty-state" v-if="sellerReviews.list.length === 0">
        <div class="empty-title">这位同学还没有收到评价</div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import axios from 'axios'
import { openAuth } from '../auth-dialog'
import { auth } from '../auth-store'
import { openDealChat } from '../deal-chat'

export default {
  name: 'ItemDetailView',
  data() {
    return {
      item: null,
      imageIndex: 0,
      messages: [],
      messageContent: '',
      replyTo: null,
      bargainVisible: false,
      bargainPrice: '',
      bargainMessage: '',
      reservationVisible: false,
      expectTime: '',
      reservationRemark: '',
      reportVisible: false,
      reportReason: 'FAKE',
      reportDetail: '',
      reviewVisible: false,
      reviewRating: 5,
      reviewContent: '',
      sellerReviewsVisible: false,
      sellerReviews: { avgRating: 0, reviewCount: 0, list: [] }
    }
  },
  computed: {
    user() {
      return auth.user
    },
    isAdminUser() {
      return !!(auth.user && auth.user.role === 'ADMIN')
    },
    currentImage() {
      return this.item && this.item.images.length > 0 ? this.item.images[this.imageIndex] : ''
    },
    saveAmount() {
      if (!this.item || !this.item.originalPrice) return 0
      const save = Number(this.item.originalPrice) - Number(this.item.price)
      return save > 0 ? save.toFixed(2) : 0
    },
    // 出价上限：低于挂牌价一分钱，与后端校验保持一致
    belowPriceText() {
      if (!this.item) return '0.00'
      const below = Number(this.item.price) - 0.01
      return below > 0 ? below.toFixed(2) : '0.00'
    },
    isOwner() {
      return !!(this.user && this.item && this.user.id === this.item.seller.id)
    }
  },
  watch: {
    // 详情页之间互相跳转时 Vue 会复用组件，必须按新 id 重新拉详情
    '$route.params.id'(id) {
      if (id) {
        this.imageIndex = 0
        this.loadDetail()
      }
    }
  },
  created() {
    this.loadMessages()
    // 首页卡片快捷操作可带 action 直接拉起弹窗，等详情拿到后再判断
    this.loadDetail().then(() => {
      // 「联系卖家 / 联系买家」从购买记录或交易单跳进来，直接定位到问答区
      if (this.$route.query.contact) {
        if (!auth.user) {
          openAuth('login')
        } else {
          this.contactCounterpart()
        }
        return
      }
      const action = this.$route.query.action
      if (action && !auth.user) {
        // 未登录时先弹出登录框，登录后按钮自然出现
        openAuth('login')
      } else if (action === 'direct' && this.item.canDirectBuy) {
        this.directBuy()
      } else if (action === 'bargain' && this.item.canDirectBuy) {
        this.bargainVisible = true
      } else if (action === 'reservation' && this.item.canDirectBuy) {
        this.reservationVisible = true
      }
    }).catch(() => {})
  },
  methods: {
    openAuth,
    openSellerReviews() {
      this.sellerReviewsVisible = true
      axios.get('/api/users/' + this.item.seller.id + '/reviews').then(res => {
        if (res.data.code === 0) {
          this.sellerReviews = res.data.data
        }
      })
    },
    ownerStatus(status) {
      const text = status === 'ON_SALE' ? '重新上架' : (status === 'SOLD' ? '线下已售' : '下架')
      const tip = status === 'SOLD'
        ? '确定要标记「线下已售」吗？该操作不可撤销，不会产生交易记录，双方也无法互相评价。'
        : '确定要' + text + '吗？'
      this.$confirm(tip, '提示').then(() => {
        axios.patch('/api/items/' + this.item.id + '/status', { status }).then(res => {
          if (res.data.code === 0) {
            this.$message.success(text + '成功')
            this.loadDetail()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    // 立即购买：按挂牌价生成待成交交易单，随后双方线下当面确认
    directBuy() {
      this.$confirm('将按挂牌价 ￥' + this.item.price + ' 生成待成交交易单，确认继续吗？', '立即购买').then(() => {
        axios.post('/api/deals/direct', { itemId: this.item.id }).then(res => {
          if (res.data.code === 0) {
            this.$message.success('已生成待成交交易单，线下交易后请回来确认成交')
            this.loadDetail()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    dealAct(action) {
      const text = action === 'complete' ? '确认成交' : '取消交易单'
      this.$confirm('确定要' + text + '吗？', '提示').then(() => {
        axios.post('/api/deals/' + this.item.dealId + '/' + action).then(res => {
          if (res.data.code === 0) {
            this.$message.success(text + '成功')
            this.loadDetail()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    // 待成交 / 已完成交易单里一键联系对方：打开交易单私密会话（与公开问答分离）
    contactCounterpart() {
      openDealChat({
        id: this.item.dealId,
        itemTitle: this.item.title,
        status: this.item.dealStatus
      })
    },
    loadDetail() {
      return axios.get('/api/items/' + this.$route.params.id).then(res => {
        if (res.data.code === 0) {
          this.item = res.data.data
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    loadMessages() {
      axios.get('/api/items/' + this.$route.params.id + '/messages').then(res => {
        if (res.data.code === 0) this.messages = res.data.data
      })
    },
    postMessage() {
      if (!this.messageContent.trim()) {
        this.$message.warning('留言内容不能为空')
        return
      }
      axios.post('/api/items/' + this.item.id + '/messages', {
        content: this.messageContent,
        parentId: this.replyTo
      }).then(res => {
        if (res.data.code === 0) {
          this.messageContent = ''
          this.replyTo = null
          this.loadMessages()
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    // 平台内联系卖家：已有交易单走私密会话，未成交走商品问答，不公开手机号
    contactSeller() {
      if (!auth.user) {
        openAuth('login')
        return
      }
      if (this.item.dealId && this.item.isBuyer) {
        openDealChat({ id: this.item.dealId, itemTitle: this.item.title, status: this.item.dealStatus })
        return
      }
      const panel = document.getElementById('qa')
      if (panel) panel.scrollIntoView({ behavior: 'smooth', block: 'start' })
      this.$nextTick(() => {
        const input = this.$refs.messageInput
        if (input && input.focus) input.focus()
      })
    },
    submitBargain() {
      const offer = Number(this.bargainPrice)
      const price = Number(this.item.price)
      const floor = Number(this.item.bargainFloor)
      if (!offer || offer <= 0) {
        this.$message.warning('请输入出价金额')
        return
      }
      if (offer >= price) {
        this.$message.warning('出价需低于挂牌价 ￥' + price.toFixed(2))
        return
      }
      if (offer < floor) {
        this.$message.warning('出价不能低于挂牌价 8 折（￥' + floor.toFixed(2) + '）')
        return
      }
      axios.post('/api/bargains', {
        itemId: this.item.id, offerPrice: this.bargainPrice, message: this.bargainMessage
      }).then(res => {
        if (res.data.code === 0) {
          this.bargainVisible = false
          this.$message.success('出价已提交，等待卖家处理')
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    discountPrice(rate) {
      if (!this.item) return '0.00'
      return (Number(this.item.price) * rate).toFixed(2)
    },
    fillBargain(rate) {
      this.bargainPrice = this.discountPrice(rate)
    },
    submitReservation() {
      axios.post('/api/reservations', {
        itemId: this.item.id, expectTime: this.expectTime || null, remark: this.reservationRemark
      }).then(res => {
        if (res.data.code === 0) {
          this.reservationVisible = false
          this.$message.success('预约已提交')
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    submitReport() {
      axios.post('/api/reports', {
        itemId: this.item.id, reason: this.reportReason, detail: this.reportDetail
      }).then(res => {
        if (res.data.code === 0) {
          this.reportVisible = false
          this.$message.success('举报已提交，管理员会尽快处理')
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    submitReview() {
      axios.post('/api/reviews', {
        itemId: this.item.id, rating: this.reviewRating, content: this.reviewContent
      }).then(res => {
        if (res.data.code === 0) {
          this.reviewVisible = false
          this.$message.success('评价已提交')
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    maskStudentNo(no) {
      return no ? no.slice(0, 4) + '****' + no.slice(-2) : ''
    },
    campusText(campus) {
      if (campus === 'DONGFENG') return '东风校区'
      return '科学校区'
    },
    conditionText(level) {
      if (level === 'NEW') return '全新'
      if (level === 'NEARLY_NEW') return '9 成新'
      return '8 成新'
    },
    statusText(status) {
      if (status === 'ON_SALE') return '在售'
      if (status === 'SOLD') return '已售'
      return '已下架'
    },
    dealSourceText(source) {
      return { DIRECT: '立即购买成交', BARGAIN: '砍价成交', RESERVATION: '预约成交' }[source] || '交易单成交'
    }
  }
}
</script>
