<template>
  <div class="main-container">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的发布</h1>
        <p class="page-sub">共 {{ total }} 件商品，可按状态筛选并调整在售状态</p>
      </div>
      <el-button type="primary" @click="openPublish">发布新商品</el-button>
    </div>

    <div class="empty-state panel" v-if="!user">
      <div class="empty-title">登录后才能管理自己的商品</div>
      <el-button type="primary" @click="openAuth('login')">去登录</el-button>
    </div>

    <div class="panel" v-else>
      <div class="panel-body">
        <div class="toolbar" style="margin-bottom: 12px">
          <el-radio-group v-model="status" @change="search">
            <el-radio-button value="ALL">全部</el-radio-button>
            <el-radio-button value="ON_SALE">在售</el-radio-button>
            <el-radio-button value="OFF_SHELF">已下架</el-radio-button>
            <el-radio-button value="SOLD">已售</el-radio-button>
          </el-radio-group>
          <span class="sep" />
          <el-button text @click="loadMyItems">刷新</el-button>
        </div>

        <el-table :data="items" v-loading="loading" style="width: 100%">
          <el-table-column label="商品" min-width="220">
            <template #default="scope">
              <div style="display: flex; gap: 10px; align-items: center">
                <img v-if="scope.row.coverImage" :src="scope.row.coverImage" style="width: 48px; height: 36px; object-fit: cover; border-radius: 4px" />
                <el-button link type="primary" @click="$router.push('/item/' + scope.row.id)">{{ scope.row.title }}</el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="价格" width="100">
            <template #default="scope"><span style="color: var(--price); font-weight: 600">￥{{ scope.row.price }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <span class="tag" :class="scope.row.status === 'ON_SALE' ? 'onsale' : (scope.row.status === 'SOLD' ? 'sold' : 'off')">
                {{ statusText(scope.row.status) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="90" />
          <el-table-column label="操作" width="200">
            <template #default="scope">
              <template v-if="scope.row.status === 'ON_SALE'">
                <el-button link type="primary" :disabled="scope.row.reserved" @click="changeStatus(scope.row, 'OFF_SHELF')">下架</el-button>
                <el-button link type="warning" :disabled="scope.row.reserved" @click="changeStatus(scope.row, 'SOLD')">线下已售</el-button>
              </template>
              <el-button v-if="scope.row.status === 'OFF_SHELF'" link type="primary" @click="changeStatus(scope.row, 'ON_SALE')">
                重新上架
              </el-button>
              <el-button link @click="$router.push('/item/' + scope.row.id)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="empty-state" v-if="!loading && items.length === 0">
          <div class="empty-title">这里还没有商品</div>
          <div>发布一件闲置，让它出现在集市里</div>
          <el-button type="primary" style="margin-top: 10px" @click="openPublish">去发布</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { openPublish } from '../publish-dialog'
import { openAuth } from '../auth-dialog'
import { auth } from '../auth-store'

export default {
  name: 'MyItemsView',
  data() {
    return {
      items: [],
      status: 'ALL',
      pageNum: 1,
      pageSize: 50,
      total: 0,
      loading: false
    }
  },
  created() {
    if (this.user) this.loadMyItems()
  },
  computed: {
    user() {
      return auth.user
    }
  },
  watch: {
    // 弹窗登录成功后自动加载列表，无需刷新页面
    user(u) {
      if (u) this.loadMyItems()
    }
  },
  methods: {
    openPublish,
    openAuth,
    loadMyItems() {
      this.loading = true
      axios.get('/api/items/mine', {
        params: { status: this.status, pageNum: this.pageNum, pageSize: this.pageSize }
      }).then(res => {
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
      this.loadMyItems()
    },
    changeStatus(row, status) {
      const text = status === 'ON_SALE' ? '重新上架' : (status === 'SOLD' ? '线下已售' : '下架')
      const tip = status === 'SOLD'
        ? '确定要把「' + row.title + '」标记为线下已售吗？该操作不可撤销，不会产生交易记录，双方也无法互相评价。'
        : '确定要' + text + '「' + row.title + '」吗？'
      this.$confirm(tip, '提示').then(() => {
        axios.patch('/api/items/' + row.id + '/status', { status }).then(res => {
          if (res.data.code === 0) {
            this.$message.success(text + '成功')
            // 就地更新行，避免整表刷新导致视线丢失
            if (this.status === 'ALL' || this.status === status) {
              row.status = status
            } else {
              this.items = this.items.filter(i => i.id !== row.id)
              this.total = Math.max(0, this.total - 1)
            }
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    },
    statusText(status) {
      return { ON_SALE: '在售', SOLD: '已售', OFF_SHELF: '已下架' }[status] || status
    }
  }
}
</script>
