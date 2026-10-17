<template>
  <div class="mine">
    <h2>我的发布</h2>
    <div class="tip" v-if="!user">请先去登录页登录</div>
    <template v-else>
      <el-radio-group v-model="status" @change="search">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="ON_SALE">在售</el-radio-button>
        <el-radio-button value="OFF_SHELF">已下架</el-radio-button>
        <el-radio-button value="SOLD">已售</el-radio-button>
      </el-radio-group>

      <el-table :data="items" style="width: 100%; margin-top: 15px">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="price" label="价格" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            {{ statusText(scope.row.status) }}
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'ON_SALE'"
              link
              type="primary"
              @click="changeStatus(scope.row, 'OFF_SHELF')"
            >下架</el-button>
            <el-button
              v-if="scope.row.status === 'ON_SALE'"
              link
              type="warning"
              @click="changeStatus(scope.row, 'SOLD')"
            >标记已售</el-button>
            <el-button
              v-if="scope.row.status === 'OFF_SHELF'"
              link
              type="primary"
              @click="changeStatus(scope.row, 'ON_SALE')"
            >重新上架</el-button>
          </template>
        </el-table-column>
      </el-table>

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
    </template>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'MyItemsView',
  data() {
    return {
      user: null,
      items: [],
      status: 'ALL',
      pageNum: 1,
      pageSize: 12,
      total: 0
    }
  },
  created() {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      this.user = JSON.parse(userStr)
      this.loadMyItems()
    }
  },
  methods: {
    loadMyItems() {
      axios.get('/api/items/mine', {
        params: {
          userId: this.user.id,
          status: this.status,
          pageNum: this.pageNum,
          pageSize: this.pageSize
        }
      }).then(res => {
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
      this.loadMyItems()
    },
    changePage(page) {
      this.pageNum = page
      this.loadMyItems()
    },
    // 下架 / 重新上架 / 标记已售（AC-5.2、5.3）
    changeStatus(row, status) {
      const actionText = status === 'ON_SALE' ? '重新上架' : (status === 'SOLD' ? '标记已售' : '下架')
      this.$confirm('确定要' + actionText + '「' + row.title + '」吗？', '提示').then(() => {
        axios.patch('/api/items/' + row.id + '/status', { status: status }, {
          params: { userId: this.user.id }
        }).then(res => {
          if (res.data.code === 0) {
            this.$message.success(actionText + '成功')
            this.loadMyItems()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {
      })
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
.mine {
  padding: 20px 24px;
}

.tip {
  margin-top: 20px;
  color: #999;
}

.pager {
  margin-top: 20px;
}
</style>
