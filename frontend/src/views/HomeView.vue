<template>
  <div class="main-container">
    <!-- 筛选台：集市的第一控件，替代宣传首屏与分类侧栏 -->
    <section class="ch-board">
      <div class="ch-board-top">
        <h1 class="ch-board-title">闲置集市</h1>
        <div class="ch-tabs">
          <button class="ch-tab" :class="{ active: status === 'ON_SALE' }" @click="setStatus('ON_SALE')">
            在售中 <span class="cnt">{{ stats.onSaleCount }}</span>
          </button>
          <button class="ch-tab" :class="{ active: status === 'SOLD' }" @click="setStatus('SOLD')">
            已售出 <span class="cnt">{{ stats.soldCount }}</span>
          </button>
        </div>
        <div class="ch-board-right">
          <span class="ch-sort-name">排序</span>
          <div class="ch-sort">
            <button
              v-for="opt in sortOptions"
              :key="opt.value"
              class="ch-sort-btn"
              :class="{ active: sort === opt.value }"
              @click="setSort(opt.value)"
            >{{ opt.label }}</button>
          </div>
        </div>
      </div>

      <div class="ch-board-filter">
        <div class="ch-filter-row">
          <span class="ch-filter-name">分类</span>
          <div class="ch-filter-body">
            <button class="ch-cat-chip" :class="{ active: categoryId === null }" @click="selectCategory(null)">
              全部<span class="n">{{ stats.onSaleCount }}</span>
            </button>
            <button
              v-for="c in categories"
              :key="c.id"
              class="ch-cat-chip"
              :class="{ active: categoryId === c.id }"
              @click="selectCategory(c.id)"
            >
              {{ c.name }}<span class="n">{{ c.onSaleCount }}</span>
            </button>
          </div>
        </div>

        <div class="ch-filter-row">
          <span class="ch-filter-name">成色</span>
          <div class="ch-filter-body">
            <el-radio-group v-model="conditionLevel" size="small" @change="search">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button value="NEW">全新</el-radio-button>
              <el-radio-button value="NEARLY_NEW">9 成新</el-radio-button>
              <el-radio-button value="GOOD">8 成新</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="ch-filter-row">
          <span class="ch-filter-name">校区</span>
          <div class="ch-filter-body">
            <el-radio-group v-model="campus" size="small" @change="search">
              <el-radio-button value="">全部校区</el-radio-button>
              <el-radio-button value="DONGFENG">东风校区</el-radio-button>
              <el-radio-button value="KEXUE">科学校区</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="ch-filter-row" v-if="keyword">
          <span class="ch-filter-name">关键词</span>
          <div class="ch-filter-body">
            <span class="tag">{{ keyword }}</span>
            <button class="ch-reset" @click="clearKeyword">清除关键词</button>
          </div>
        </div>

        <div class="ch-filter-row">
          <span class="ch-filter-name" />
          <div class="ch-filter-body">
            <span class="ch-result-hint">共 {{ total }} 件符合条件</span>
            <button class="ch-reset" style="margin-left: auto" @click="resetFilters">重置全部筛选</button>
          </div>
        </div>
      </div>
    </section>

    <div class="ch-trustline">
      <span><i />学号实名认证</span>
      <span><i />同校当面交易</span>
      <span><i />成交后互评信用</span>
      <span class="right">仅面向郑州轻工业大学：东风校区 / 科学校区</span>
    </div>

    <div class="ch-result">
      <h2>{{ status === 'SOLD' ? '已成交闲置' : '在售闲置' }}</h2>
      <span class="hint"><b class="n">{{ total }}</b> 件</span>
      <span class="hint" v-if="activeSummary">· {{ activeSummary }}</span>
    </div>

    <div class="ch-goods-grid" v-if="items.length > 0" v-loading="loading">
      <div class="goods-card" v-for="item in items" :key="item.id" @click="$router.push('/item/' + item.id)">
        <div class="goods-cover">
          <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" loading="lazy" />
          <span class="goods-badge cond" :class="item.conditionLevel">{{ conditionText(item.conditionLevel) }}</span>
          <span class="goods-badge discount" v-if="discountText(item)">{{ discountText(item) }}</span>
          <span class="goods-badge reserved" v-if="item.reserved && item.status === 'ON_SALE'">待成交</span>
          <div class="sold-mask" v-if="item.status === 'SOLD'"><span class="sold-stamp">已售</span></div>
          <div class="quick-actions" v-if="item.status === 'ON_SALE' && !item.reserved && !isAdmin">
            <el-button size="small" type="primary" @click.stop="$router.push('/item/' + item.id + '?action=direct')">购买</el-button>
            <el-button size="small" @click.stop="$router.push('/item/' + item.id + '?action=bargain')">出价</el-button>
            <el-button size="small" @click.stop="$router.push('/item/' + item.id + '?action=reservation')">预约</el-button>
          </div>
        </div>
        <div class="goods-body">
          <div class="goods-title">{{ item.title }}</div>
          <div class="goods-desc">{{ item.description || '卖家未填写详细描述' }}</div>
          <div class="goods-price">
            <span class="unit">￥</span>{{ item.price }}
            <span class="origin" v-if="item.originalPrice">￥{{ item.originalPrice }}</span>
          </div>
          <div class="goods-meta">
            <span class="seller">{{ item.sellerName }} · {{ campusText(item.campus) }}</span>
            <span>{{ timeAgo(item.createdAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel empty-state" v-else-if="!loading">
      <div class="empty-title">{{ status === 'SOLD' ? '暂时还没有成交记录' : '没有符合条件的闲置商品' }}</div>
      <div>换个筛选条件试试</div>
    </div>

    <div style="text-align: center; margin-top: 18px" v-if="items.length > 0 && items.length < total">
      <el-button :loading="loading" @click="loadMore">加载更多（还有 {{ total - items.length }} 件）</el-button>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { auth } from '../auth-store'

export default {
  name: 'HomeView',
  data() {
    return {
      items: [],
      categories: [],
      stats: { onSaleCount: 0, soldCount: 0, certifiedUserCount: 0, totalViewCount: 0 },
      keyword: '',
      categoryId: null,
      conditionLevel: '',
      campus: '',
      sort: 'latest',
      status: 'ON_SALE',
      pageNum: 1,
      pageSize: 40,
      total: 0,
      loading: false,
      sortOptions: [
        { label: '最新', value: 'latest' },
        { label: '价格低', value: 'priceAsc' },
        { label: '价格高', value: 'priceDesc' },
        { label: '最多浏览', value: 'views' }
      ]
    }
  },
  computed: {
    isAdmin() {
      return !!(auth.user && auth.user.role === 'ADMIN')
    },
    activeSummary() {
      const parts = []
      if (this.categoryId !== null) {
        const c = this.categories.find(x => x.id === this.categoryId)
        if (c) parts.push(c.name)
      }
      if (this.conditionLevel) parts.push(this.conditionText(this.conditionLevel))
      if (this.campus) parts.push(this.campusText(this.campus))
      if (this.keyword) parts.push('“' + this.keyword + '”')
      return parts.join(' · ')
    }
  },
  created() {
    this.applyQuery()
    this.loadCategories()
    this.loadStats()
    this.loadItems()
  },
  mounted() {
    window.addEventListener('campushub:search', this.search)
  },
  beforeUnmount() {
    window.removeEventListener('campushub:search', this.search)
  },
  watch: {
    '$route.query'() {
      if (this.$route.path !== '/') return
      if (this.applyQuery()) this.search()
    }
  },
  methods: {
    applyQuery() {
      const q = this.$route.query
      const next = {
        keyword: q.keyword || '',
        categoryId: q.categoryId ? Number(q.categoryId) : null,
        conditionLevel: q.conditionLevel || '',
        campus: q.campus || '',
        sort: q.sort || 'latest',
        status: q.status || 'ON_SALE'
      }
      const changed = next.keyword !== this.keyword || next.categoryId !== this.categoryId
        || next.conditionLevel !== this.conditionLevel || next.campus !== this.campus
        || next.sort !== this.sort || next.status !== this.status
      Object.assign(this, next)
      return changed
    },
    syncQuery() {
      const q = {}
      if (this.keyword) q.keyword = this.keyword
      if (this.categoryId !== null) q.categoryId = String(this.categoryId)
      if (this.conditionLevel) q.conditionLevel = this.conditionLevel
      if (this.campus) q.campus = this.campus
      if (this.sort !== 'latest') q.sort = this.sort
      if (this.status !== 'ON_SALE') q.status = this.status
      this.$router.replace({ path: '/', query: q }).catch(() => {})
    },
    loadStats() {
      axios.get('/api/system/stats').then(res => {
        if (res.data.code === 0) this.stats = res.data.data
      })
    },
    loadCategories() {
      axios.get('/api/categories').then(res => {
        if (res.data.code === 0) this.categories = res.data.data
      })
    },
    loadItems(append) {
      const params = { status: this.status, sort: this.sort, pageNum: this.pageNum, pageSize: this.pageSize }
      if (this.keyword) params.keyword = this.keyword
      if (this.categoryId !== null) params.categoryId = this.categoryId
      if (this.conditionLevel) params.conditionLevel = this.conditionLevel
      if (this.campus) params.campus = this.campus
      this.loading = true
      axios.get('/api/items', { params }).then(res => {
        if (res.data.code === 0) {
          const list = res.data.data.list
          this.items = append ? this.items.concat(list) : list
          this.total = res.data.data.total
        }
      }).finally(() => {
        this.loading = false
      })
    },
    // 集市规模不大，默认一次性铺开；继续增长时用「加载更多」而不是翻页
    loadMore() {
      this.pageNum += 1
      this.loadItems(true)
    },
    search() {
      this.pageNum = 1
      this.syncQuery()
      this.loadItems(false)
    },
    clearKeyword() {
      this.keyword = ''
      this.search()
    },
    setStatus(status) {
      this.status = status
      this.search()
    },
    setSort(sort) {
      this.sort = sort
      this.search()
    },
    selectCategory(id) {
      this.categoryId = id
      this.search()
    },
    resetFilters() {
      this.keyword = ''
      this.categoryId = null
      this.conditionLevel = ''
      this.campus = ''
      this.sort = 'latest'
      this.search()
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
    discountText(item) {
      if (!item.originalPrice || Number(item.originalPrice) <= Number(item.price)) return ''
      const off = Math.round((1 - Number(item.price) / Number(item.originalPrice)) * 10)
      return off > 0 ? off + ' 折' : ''
    },
    timeAgo(time) {
      if (!time) return ''
      const diff = (Date.now() - new Date(time).getTime()) / 1000
      if (diff < 3600) return Math.max(1, Math.floor(diff / 60)) + ' 分钟前'
      if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
      return Math.floor(diff / 86400) + ' 天前'
    }
  }
}
</script>

<style scoped>
.ch-result-hint { font-size: 13px; color: var(--text-muted); }
</style>
