<template>
  <div class="home">
    <h2>闲置集市</h2>
    <div class="item-list">
      <div class="item-card" v-for="item in items" :key="item.id">
        <div class="title">{{ item.title }}</div>
        <div class="price">￥{{ item.price }}</div>
        <div class="info">{{ item.categoryName }} · {{ campusText(item.campus) }} · {{ item.sellerName }}</div>
      </div>
    </div>
    <div class="empty" v-if="items.length === 0">暂无商品</div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'HomeView',
  data() {
    return {
      items: []
    }
  },
  created() {
    this.loadItems()
  },
  methods: {
    loadItems() {
      axios.get('/api/items').then(res => {
        if (res.data.code === 0) {
          this.items = res.data.data
        } else {
          this.$message.error(res.data.message)
        }
      })
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
    }
  }
}
</script>

<style scoped>
.home {
  padding: 20px 24px;
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
}

.item-card .title {
  font-size: 16px;
  font-weight: bold;
}

.item-card .price {
  margin: 8px 0;
  color: #e4393c;
  font-size: 18px;
}

.item-card .info {
  color: #888;
  font-size: 12px;
}

.empty {
  margin-top: 30px;
  color: #999;
  text-align: center;
}
</style>
