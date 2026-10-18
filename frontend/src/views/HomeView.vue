<template>
  <div class="home">
    <div class="page-head">
      <div>
        <h1 class="page-title">闲置集市</h1>
        <p class="page-sub">按校区、成色和分类快速找到合适的二手好物</p>
      </div>
      <el-button type="primary" @click="$router.push('/publish')">发布闲置</el-button>
    </div>

    <div class="stat-strip">
      <div class="stat-cell brand">
        <div class="value">{{ stats.onSaleCount }}</div>
        <div class="label">在售商品</div>
      </div>
      <div class="stat-cell price">
        <div class="value">{{ stats.soldCount }}</div>
        <div class="label">累计成交</div>
      </div>
      <div class="stat-cell success">
        <div class="value">{{ stats.certifiedUserCount }}</div>
        <div class="label">认证同学</div>
      </div>
      <div class="stat-cell warning">
        <div class="value">{{ stats.totalViewCount }}</div>
        <div class="label">累计浏览</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-body">
        <div class="toolbar">
          <el-input
            v-model="keyword"
            class="grow"
            placeholder="搜索标题或描述"
            clearable
            @keyup.enter="search"
            @clear="search"
          >
            <template #append>
              <el-button type="primary" @click="search">搜索</el-button>
            </template>
          </el-input>
          <el-select v-model="campus" placeholder="校区" clearable style="width: 128px" @change="search">
            <el-option label="主校区" value="MAIN" />
            <el-option label="南湖校区" value="NANHU" />
            <el-option label="西校区" value="WEST" />
          </el-select>
          <el-select v-model="conditionLevel" placeholder="成色" clearable style="width: 128px" @change="search">
            <el-option label="全新" value="NEW" />
            <el-option label="9 成新" value="NEARLY_NEW" />
            <el-option label="8 成新" value="GOOD" />
          </el-select>
          <el-select v-model="sort" style="width: 140px" @change="search">
            <el-option label="最新发布" value="latest" />
            <el-option label="价格从低到高" value="priceAsc" />
            <el-option label="价格从高到低" value="priceDesc" />
            <el-option label="最多浏览" value="views" />
          </el-select>
        </div>

        <div class="chip-row" style="margin-top: 14px">
          <span class="chip" :class="{ active: categoryId === null }" @click="selectCategory(null)">全部分类</span>
          <span
            class="chip"
            v-for="c in categories"
            :key="c.id"
            :class="{ active: categoryId === c.id }"
            @click="selectCategory(c.id)"
          >
            {{ c.name }}<span class="count">{{ c.onSaleCount }}</span>
          </span>
        </div>

        <div class="toolbar" style="margin-top: 14px">
          <el-radio-group v-model="status" @change="search">
            <el-radio-button value="ON_SALE">在售</el-radio-button>
            <el-radio-button value="SOLD">已售</el-radio-button>
          </el-radio-group>
          <span class="sep" />
          <span class="page-sub" style="margin: 0">共 {{ total }} 件</span>
        </div>
      </div>
    </div>

    <div v-loading="loading" style="margin-top: 14px; min-height: 200px">
      <div class="goods-grid" v-if="items.length > 0">
        <div class="goods-card" v-for="item in items" :key="item.id" @click="$router.push('/item/' + item.id)">
          <div class="goods-cover">
            <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" />
            <span class="goods-badge" v-if="item.status === 'SOLD'">已售</span>
            <span class="goods-badge" v-else-if="item.reserved">已预约</span>
          </div>
          <div class="goods-body">
            <div class="goods-title">{{ item.title }}</div>
            <div class="goods-price"><span class="unit">￥</span>{{ item.price }}</div>
            <div class="goods-meta">
              <span>{{ campusText(item.campus) }} · {{ conditionText(item.conditionLevel) }}</span>
              <span>{{ item.viewCount }} 次浏览</span>
            </div>
            <div class="goods-meta" style="margin-top: 4px">
              <span>{{ item.categoryName }}</span>
              <span>{{ item.sellerName }}</span>
            </div>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else-if="!loading">
        <div class="empty-title">没有找到匹配的商品</div>
        <div>试试更换分类或清空筛选条件</div>
      </div>
    </div>

    <el-pagination
      v-if="total > pageSize"
      style="margin-top: 20px; justify-content: center"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="changePage"
    />
  </div>
</template>

<script>
import axios from 'axios'

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
      pageSize: 12,
      total: 0,
      loading: false
    }
  },
  created() {
    this.loadCategories()
    this.loadStats()
    this.loadItems()
  },
  methods: {
    loadStats() {
      axios.get('/api/system/stats').then(res => {
        if (res.data.code === 0) {
          this.stats = res.data.data
        }
      })
    },
    loadCategories() {
      axios.get('/api/categories').then(res => {
        if (res.data.code === 0) {
          this.categories = res.data.data
        }
      })
    },
    loadItems() {
      const params = {
        status: this.status,
        sort: this.sort,
        pageNum: this.pageNum,
        pageSize: this.pageSize
      }
      if (this.keyword) params.keyword = this.keyword
      if (this.categoryId !== null) params.categoryId = this.categoryId
      if (this.conditionLevel) params.conditionLevel = this.conditionLevel
      if (this.campus) params.campus = this.campus
      this.loading = true
      axios.get('/api/items', { params }).then(res => {
        if (res.data.code === 0) {
          this.items = res.data.data.list
          this.total = res.data.data.total
        } else {
          this.$message.error(res.data.message)
        }
      }).finally(() => {
        this.loading = false
      })
    },
    search() {
      this.pageNum = 1
      this.loadItems()
    },
    selectCategory(id) {
      this.categoryId = id
      this.search()
    },
    changePage(page) {
      this.pageNum = page
      this.loadItems()
    },
    campusText(campus) {
      if (campus === 'MAIN') return '主校区'
      if (campus === 'NANHU') return '南湖校区'
      return '西校区'
    },
    conditionText(level) {
      if (level === 'NEW') return '全新'
      if (level === 'NEARLY_NEW') return '9 成新'
      return '8 成新'
    }
  }
}
</script>
