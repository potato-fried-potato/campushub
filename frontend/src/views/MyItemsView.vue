<template>
  <div class="mine">
    <h2>我的发布</h2>
    <div class="tip" v-if="!user">请先去登录页登录</div>
    <el-table v-else :data="items" style="width: 100%">
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="price" label="价格" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          {{ statusText(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览量" width="100" />
    </el-table>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'MyItemsView',
  data() {
    return {
      user: null,
      items: []
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
      axios.get('/api/items/mine', { params: { userId: this.user.id } }).then(res => {
        if (res.data.code === 0) {
          this.items = res.data.data
        } else {
          this.$message.error(res.data.message)
        }
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
</style>
