<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-sub">共 {{ total }} 个账号，可禁用或恢复</p>
      </div>
    </div>
    <div class="panel">
      <div class="panel-body">
        <div class="toolbar" style="margin-bottom: 12px">
          <el-input v-model="keyword" class="grow" placeholder="按学号或昵称搜索" clearable @keyup.enter="load" />
          <el-select v-model="status" placeholder="状态" clearable style="width: 130px" @change="search">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已禁用" value="DISABLED" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
        </div>
        <el-table :data="users" v-loading="loading" empty-text="没有匹配的用户">
          <el-table-column prop="studentNo" label="学号" width="150" />
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column prop="phone" label="联系电话" width="140" />
          <el-table-column label="角色" width="90">
            <template #default="scope">
              <span class="tag" :class="scope.row.role === 'ADMIN' ? 'onsale' : ''">
                {{ scope.row.role === 'ADMIN' ? '管理员' : '学生' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <span class="tag" :class="scope.row.status === 'ACTIVE' ? 'onsale' : 'sold'">
                {{ scope.row.status === 'ACTIVE' ? '正常' : '已禁用' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="scope">
              <el-button
                link
                :type="scope.row.status === 'ACTIVE' ? 'danger' : 'primary'"
                :disabled="scope.row.role === 'ADMIN'"
                @click="toggle(scope.row)"
              >{{ scope.row.status === 'ACTIVE' ? '禁用' : '启用' }}</el-button>
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
  name: 'AdminUsersView',
  data() {
    return { keyword: '', status: '', users: [], total: 0, pageNum: 1, pageSize: 20, loading: false }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      axios.get('/api/admin/users', {
        params: { keyword: this.keyword, status: this.status, pageNum: this.pageNum, pageSize: this.pageSize }
      }).then(res => {
        if (res.data.code === 0) {
          this.users = res.data.data.list
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
    toggle(row) {
      const status = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
      this.$confirm((status === 'ACTIVE' ? '启用' : '禁用') + '「' + row.nickname + '」？', '提示').then(() => {
        axios.patch('/api/admin/users/' + row.id + '/status', { status }).then(res => {
          if (res.data.code === 0) {
            this.$message.success('操作成功')
            this.load()
          } else {
            this.$message.error(res.data.message)
          }
        })
      }).catch(() => {})
    }
  }
}
</script>
