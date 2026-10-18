<template>
  <div class="page-box">
    <div class="page-head">
      <div>
        <h1 class="page-title">我的信用</h1>
        <p class="page-sub">来自交易对方的评价记录</p>
      </div>
    </div>
    <div class="summary">
      <div class="score">{{ data.avgRating }}</div>
      <div class="label">平均评分 · 共 {{ data.reviewCount }} 条评价</div>
    </div>
    <el-table :data="data.list" style="width: 100%; margin-top: 15px">
      <el-table-column prop="reviewerNickname" label="评价人" width="120" />
      <el-table-column label="评分" width="100">
        <template #default="scope">{{ scope.row.rating }} 星</template>
      </el-table-column>
      <el-table-column prop="content" label="评价内容" />
      <el-table-column prop="createdAt" label="时间" width="180" />
    </el-table>
    <div class="empty" v-if="data.list.length === 0">暂无评价</div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'CreditView',
  data() {
    return { data: { avgRating: 0, reviewCount: 0, list: [] } }
  },
  created() {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (!user) {
      this.$message.warning('请先登录')
      return
    }
    axios.get('/api/users/' + user.id + '/reviews').then(res => {
      if (res.data.code === 0) {
        this.data = res.data.data
      }
    })
  }
}
</script>

<style scoped>
.page-box { padding: 20px 24px; }
.summary {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 16px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
}
.score { color: #e4393c; font-size: 30px; font-weight: bold; }
.label { color: #666; font-size: 14px; }
.empty { margin-top: 30px; color: #999; text-align: center; }
</style>
