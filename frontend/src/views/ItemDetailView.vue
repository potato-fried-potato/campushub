<template>
  <div class="detail" v-if="item">
    <div class="gallery">
      <img class="main-image" :src="currentImage" :alt="item.title" />
      <div class="thumb-list" v-if="item.images.length > 1">
        <img
          v-for="(img, index) in item.images"
          :key="index"
          class="thumb"
          :class="{ active: index === imageIndex }"
          :src="img"
          @click="imageIndex = index"
        />
      </div>
    </div>

    <div class="info-box">
      <h2>{{ item.title }}</h2>
      <div class="price">
        ￥{{ item.price }}
        <span class="original" v-if="item.originalPrice">原价 ￥{{ item.originalPrice }}</span>
        <span class="save" v-if="saveAmount > 0">立省 ￥{{ saveAmount }}</span>
      </div>
      <div class="meta">
        <div>分类：{{ item.categoryName }}</div>
        <div>成色：{{ conditionText(item.conditionLevel) }}</div>
        <div>校区：{{ campusText(item.campus) }}</div>
        <div>状态：{{ statusText(item.status) }}</div>
        <div>浏览量：{{ item.viewCount }}</div>
      </div>
      <div class="description">{{ item.description }}</div>

      <div class="seller-box">
        <div class="seller-title">卖家信息</div>
        <div class="seller-line">{{ item.seller.nickname }} · 学号认证 {{ item.seller.studentNo }}</div>
        <div class="seller-line">信用评分：{{ item.seller.avgRating }} 分（{{ item.seller.reviewCount }} 条评价）</div>
        <div class="seller-line">联系电话：{{ item.seller.phone }}</div>
        <el-button type="primary" @click="copyPhone">复制联系方式</el-button>
      </div>

      <div class="actions" v-if="user">
        <template v-if="!isOwner && item.status === 'ON_SALE'">
          <el-button type="danger" @click="bargainVisible = true">出价</el-button>
          <el-button @click="reservationVisible = true">预约</el-button>
        </template>
        <el-button v-if="!isOwner" @click="reportVisible = true">举报</el-button>
        <el-button v-if="item.status === 'SOLD'" @click="reviewVisible = true">评价交易</el-button>
      </div>

      <div class="safety">
        交易安全提示：请在校内当面验货交易，不要提前转账或点击陌生链接付款。
      </div>
    </div>

    <div class="messages">
      <h3>商品留言</h3>
      <div class="topic" v-for="t in messages" :key="t.id">
        <div class="topic-line">
          <span class="who">{{ t.sender.nickname }}</span>：{{ t.content }}
          <span class="time">{{ t.createdAt }}</span>
        </div>
        <div class="reply" v-for="r in t.replies" :key="r.id">
          <span class="who">{{ r.sender.nickname }}</span>：{{ r.content }}
        </div>
        <el-button v-if="user" link type="primary" @click="replyTo = t.id">回复</el-button>
      </div>
      <div class="empty-msg" v-if="messages.length === 0">还没有留言</div>
      <div class="post-box" v-if="user">
        <el-input v-model="messageContent" placeholder="留言（1~200 字）" maxlength="200" />
        <el-button type="primary" @click="postMessage">发送</el-button>
        <el-button v-if="replyTo" @click="replyTo = null">取消回复</el-button>
      </div>
    </div>

    <el-dialog v-model="bargainVisible" title="出价" width="360px">
      <el-input v-model="bargainPrice" placeholder="出价（需低于挂牌价）" class="dialog-item" />
      <el-input v-model="bargainMessage" placeholder="留言（选填）" class="dialog-item" />
      <template #footer>
        <el-button @click="bargainVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBargain">提交出价</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reservationVisible" title="意向预约" width="380px">
      <el-input v-model="expectTime" placeholder="期望交易时间，如 2026-11-01T15:00:00" class="dialog-item" />
      <el-input v-model="reservationRemark" placeholder="备注（选填）" class="dialog-item" />
      <template #footer>
        <el-button @click="reservationVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReservation">提交预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reportVisible" title="举报商品" width="380px">
      <el-select v-model="reportReason" placeholder="举报原因" class="dialog-item">
        <el-option label="虚假信息" value="FAKE" />
        <el-option label="违禁物品" value="ILLEGAL" />
        <el-option label="诈骗风险" value="SCAM" />
        <el-option label="其他" value="OTHER" />
      </el-select>
      <el-input v-model="reportDetail" placeholder="补充说明（选填）" class="dialog-item" />
      <template #footer>
        <el-button @click="reportVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="评价交易" width="380px">
      <el-select v-model="reviewRating" class="dialog-item">
        <el-option v-for="n in 5" :key="n" :label="n + ' 星'" :value="n" />
      </el-select>
      <el-input v-model="reviewContent" type="textarea" :rows="3" placeholder="评价内容（选填）" class="dialog-item" />
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
  <div class="empty" v-else>商品不存在或已下架</div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'ItemDetailView',
  data() {
    return {
      item: null,
      imageIndex: 0,
      user: JSON.parse(localStorage.getItem('user') || 'null'),
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
      reviewContent: ''
    }
  },
  computed: {
    currentImage() {
      if (this.item && this.item.images.length > 0) {
        return this.item.images[this.imageIndex]
      }
      return ''
    },
    // 立省金额（AC-3.2）
    saveAmount() {
      if (!this.item || !this.item.originalPrice) {
        return 0
      }
      const save = Number(this.item.originalPrice) - Number(this.item.price)
      return save > 0 ? save.toFixed(2) : 0
    },
    isOwner() {
      return this.user && this.item && this.user.id === this.item.seller.id
    }
  },
  created() {
    this.loadDetail()
    this.loadMessages()
  },
  methods: {
    loadDetail() {
      axios.get('/api/items/' + this.$route.params.id).then(res => {
        if (res.data.code === 0) {
          this.item = res.data.data
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    // 一键复制联系方式（AC-3.3）
    copyPhone() {
      const phone = this.item.seller.phone
      if (navigator.clipboard) {
        navigator.clipboard.writeText(phone).then(() => {
          this.$message.success('联系方式已复制：' + phone)
        })
      } else {
        this.$message.info('联系电话：' + phone)
      }
    },
    loadMessages() {
      axios.get('/api/items/' + this.$route.params.id + '/messages').then(res => {
        if (res.data.code === 0) {
          this.messages = res.data.data
        }
      })
    },
    postMessage() {
      if (this.messageContent.trim() === '') {
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
    submitBargain() {
      axios.post('/api/bargains', {
        itemId: this.item.id,
        offerPrice: this.bargainPrice,
        message: this.bargainMessage
      }).then(res => {
        if (res.data.code === 0) {
          this.bargainVisible = false
          this.$message.success('出价已提交')
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    submitReservation() {
      axios.post('/api/reservations', {
        itemId: this.item.id,
        expectTime: this.expectTime || null,
        remark: this.reservationRemark
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
        itemId: this.item.id,
        reason: this.reportReason,
        detail: this.reportDetail
      }).then(res => {
        if (res.data.code === 0) {
          this.reportVisible = false
          this.$message.success('举报已提交')
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    submitReview() {
      axios.post('/api/reviews', {
        itemId: this.item.id,
        rating: this.reviewRating,
        content: this.reviewContent
      }).then(res => {
        if (res.data.code === 0) {
          this.reviewVisible = false
          this.$message.success('评价已提交')
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    campusText(campus) {
      if (campus === 'MAIN') {
        return '主校区'
      }
      if (campus === 'NANHU') {
        return '南湖校区'
      }
      return '西校区'
    },
    conditionText(level) {
      if (level === 'NEW') {
        return '全新'
      }
      if (level === 'NEARLY_NEW') {
        return '9 成新'
      }
      return '8 成新'
    },
    statusText(status) {
      if (status === 'ON_SALE') {
        return '在售'
      }
      if (status === 'SOLD') {
        return '已售'
      }
      return '已下架'
    }
  }
}
</script>

<style scoped>
.detail {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 20px 24px;
}

.gallery {
  width: 420px;
}

.main-image {
  width: 420px;
  height: 315px;
  object-fit: cover;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.thumb-list {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.thumb {
  width: 68px;
  height: 51px;
  object-fit: cover;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  cursor: pointer;
}

.thumb.active {
  border-color: #409eff;
}

.info-box {
  flex: 1;
  min-width: 320px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.info-box h2 {
  margin: 0 0 12px;
}

.price {
  color: #e4393c;
  font-size: 24px;
}

.price .original {
  margin-left: 10px;
  color: #999;
  font-size: 14px;
  text-decoration: line-through;
}

.price .save {
  margin-left: 10px;
  padding: 2px 8px;
  background: #fdf0f0;
  border-radius: 4px;
  color: #e4393c;
  font-size: 13px;
}

.meta {
  margin-top: 14px;
  color: #666;
  font-size: 14px;
  line-height: 26px;
}

.description {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  color: #444;
  font-size: 14px;
  line-height: 24px;
}

.seller-box {
  margin-top: 16px;
  padding: 12px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 6px;
}

.seller-title {
  margin-bottom: 6px;
  font-weight: bold;
}

.seller-line {
  color: #666;
  font-size: 13px;
  line-height: 22px;
}

.seller-box .el-button {
  margin-top: 10px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.messages {
  width: 100%;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.messages h3 {
  margin: 0 0 10px;
}

.topic {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}

.topic-line {
  font-size: 14px;
}

.who {
  font-weight: bold;
}

.time {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}

.reply {
  margin: 6px 0 0 16px;
  color: #555;
  font-size: 13px;
}

.empty-msg {
  color: #999;
  font-size: 13px;
}

.post-box {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.dialog-item {
  margin-bottom: 12px;
}

.safety {
  margin-top: 16px;
  padding: 10px 12px;
  background: #fff8e6;
  border-radius: 6px;
  color: #8a6d3b;
  font-size: 13px;
  line-height: 20px;
}

.empty {
  padding: 60px 0;
  color: #999;
  text-align: center;
}
</style>
