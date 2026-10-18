<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">商品管理</h1>
        <p class="page-sub">共 {{ total }} 件商品，可对违规商品强制下架</p>
      </div>
    </div>
    <div class="panel">
      <div class="panel-body">
        <div class="toolbar" style="margin-bottom: 12px">
          <el-radio-group v-model="status" @change="search">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="ON_SALE">在售</el-radio-button>
            <el-radio-button value="OFF_SHELF">已下架</el-radio-button>
            <el-radio-button value="SOLD">已售</el-radio-button>
          </el-radio-group>
          <el-input v-model="keyword" class="grow" placeholder="按标题搜索" clearable @keyup.enter="load" />
          <el-button type="primary" @click="search">查询</el-button>
        </div>
        <el-table :data="items" v-loading="loading" empty-text="没有匹配的商品">
          <el-table-column label="商品" min-width="220">
            <template #default="scope">
              <div style="display: flex; gap: 10px; align-items: center">
                <img v-if="scope.row.coverImage" :src="scope.row.coverImage" style="width: 48px; height: 36px; object-fit: cover; border-radius: 4px" />
                <el-button link type="primary" @click="openItem(scope.row)">{{ scope.row.title }}</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="90" />
          <el-table-column prop="sellerName" label="卖家" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <span class="tag" :class="scope.row.status === 'ON_SALE' ? 'onsale' : (scope.row.status === 'SOLD' ? 'sold' : 'off')">
                {{ statusText(scope.row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="scope">
              <el-button v-if="scope.row.status !== 'OFF_SHELF'" link type="danger" @click="offShelf(scope.row)">强制下架</el-button>
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
  name: 'AdminItemsView',
  data() {
    return { status: '', keyword: '', items: [], total: 0, pageNum: 1, pageSize: 20, loading: false }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      axios.get('/api/admin/items', {
        params: { status: this.status, keyword: this.keyword, pageNum: this.pageNum, pageSize: this.pageSize }
      }).then(res => {
        if (res.data.code === 0) {
          this.items = res.data.data.list
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
      window.open('/#/item/' + row.id, '_blank')
    },
    offShelf(row) {
      this.$confirm('强制下架「' + row.title + '」后，该商品不再出现在集市。', '提示', { type: 'warning' }).then(() => {
        axios.patch('/api/admin/items/' + row.id + '/off-shelf').then(res => {
          if (res.data.code === 0) {
            this.$message.success('已下架')
            this.load()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    statusText(status) {
      return { ON_SALE: '在售', OFF_SHELF: '已下架', SOLD: '已售' }[status] || status
    }
  }
}
</script>
