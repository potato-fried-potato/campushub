<template>
  <div class="home">
    <h2>闲置集市</h2>

    <div class="filter-box">
      <el-input v-model="keyword" placeholder="搜索标题或描述" clearable class="keyword" @keyup.enter="search" />
      <el-select v-model="conditionLevel" placeholder="成色" clearable class="select" @change="search">
        <el-option label="全新" value="NEW" />
        <el-option label="9 成新" value="NEARLY_NEW" />
        <el-option label="8 成新" value="GOOD" />
      </el-select>
      <el-select v-model="campus" placeholder="校区" clearable class="select" @change="search">
        <el-option label="主校区" value="MAIN" />
        <el-option label="南湖校区" value="NANHU" />
        <el-option label="西校区" value="WEST" />
      </el-select>
      <el-select v-model="sort" class="select" @change="search">
        <el-option label="最新发布" value="latest" />
        <el-option label="价格最低" value="priceAsc" />
        <el-option label="价格最高" value="priceDesc" />
        <el-option label="最多浏览" value="views" />
      </el-select>
      <el-button type="primary" @click="search">搜索</el-button>
    </div>

    <div class="category-box">
      <span class="category" :class="{ active: categoryId === null }" @click="selectCategory(null)">全部</span>
      <span
        class="category"
        v-for="c in categories"
        :key="c.id"
        :class="{ active: categoryId === c.id }"
        @click="selectCategory(c.id)"
      >
        {{ c.name }} {{ c.onSaleCount }}
      </span>
    </div>

    <div class="status-box">
      <el-radio-group v-model="status" @change="search">
        <el-radio-button value="ON_SALE">在售</el-radio-button>
        <el-radio-button value="SOLD">已售</el-radio-button>
      </el-radio-group>
      <span class="total">共 {{ total }} 件</span>
    </div>

    <div class="item-list">
      <div class="item-card" v-for="item in items" :key="item.id" @click="$router.push('/item/' + item.id)">
        <img v-if="item.coverImage" class="cover" :src="item.coverImage" :alt="item.title" />
        <div class="title">{{ item.title }}</div>
        <div class="price">
          ￥{{ item.price }}
          <span class="sold" v-if="item.status === 'SOLD'">已售</span>
        </div>
        <div class="info">{{ item.categoryName }} · {{ campusText(item.campus) }} · {{ conditionText(item.conditionLevel) }}</div>
        <div class="info">{{ item.sellerName }} · 浏览 {{ item.viewCount }}</div>
      </div>
    </div>
    <div class="empty" v-if="items.length === 0">暂无商品</div>

    <el-pagination
      v-if="total > pageSize"
      class="pager"
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
      keyword: '',
      categoryId: null,
      conditionLevel: '',
      campus: '',
      sort: 'latest',
      status: 'ON_SALE',
      pageNum: 1,
      pageSize: 12,
      total: 0
    }
  },
  created() {
    this.loadCategories()
    this.loadItems()
  },
  methods: {
    // 分类标签和各自在售数量（AC-2.2）
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
      if (this.keyword !== '') {
        params.keyword = this.keyword
      }
      if (this.categoryId !== null) {
        params.categoryId = this.categoryId
      }
      if (this.conditionLevel !== '') {
        params.conditionLevel = this.conditionLevel
      }
      if (this.campus !== '') {
        params.campus = this.campus
      }
      // 登录后自动排除自己发布的商品（AC-2.5）
      const userStr = localStorage.getItem('user')
      if (userStr) {
        params.excludeUserId = JSON.parse(userStr).id
      }
      axios.get('/api/items', { params }).then(res => {
        if (res.data.code === 0) {
          this.items = res.data.data.list
          this.total = res.data.data.total
        } else {
          this.$message.error(res.data.message)
        }
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
    // 古法翻译一下校区
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
    }
  }
}
</script>

<style scoped>
.home {
  padding: 20px 24px;
}

.filter-box {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}

.filter-box .keyword {
  width: 240px;
}

.filter-box .select {
  width: 120px;
}

.category-box {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.category-box .category {
  padding: 5px 12px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  color: #555;
  font-size: 13px;
  cursor: pointer;
}

.category-box .category.active {
  border-color: #409eff;
  color: #409eff;
}

.status-box {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-top: 15px;
}

.status-box .total {
  color: #888;
  font-size: 13px;
}

.item-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-top: 15px;
}

.item-card {
  width: 220px;
  padding: 12px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  cursor: pointer;
}

.item-card .cover {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 4px;
}

.item-card .title {
  margin-top: 8px;
  font-size: 16px;
  font-weight: bold;
}

.item-card .price {
  margin: 8px 0;
  color: #e4393c;
  font-size: 18px;
}

.item-card .price .sold {
  margin-left: 6px;
  padding: 1px 6px;
  background: #999;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
}

.item-card .info {
  color: #888;
  font-size: 12px;
  line-height: 20px;
}

.empty {
  margin-top: 30px;
  color: #999;
  text-align: center;
}

.pager {
  margin-top: 20px;
}
</style>
