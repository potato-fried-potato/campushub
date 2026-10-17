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

      <div class="safety">
        交易安全提示：请在校内当面验货交易，不要提前转账或点击陌生链接付款。
      </div>
    </div>
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
      imageIndex: 0
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
    }
  },
  created() {
    this.loadDetail()
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
