<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="app-header-inner">
        <router-link to="/" class="brand">
          <span class="brand-mark">智</span>
          <span>智校互联</span>
        </router-link>

        <nav class="app-nav">
          <router-link to="/">闲置集市</router-link>
          <router-link to="/publish">发布闲置</router-link>
          <router-link to="/bargains">砍价</router-link>
          <router-link to="/reservations">预约</router-link>
          <router-link to="/messages">留言</router-link>
          <router-link to="/credit">信用</router-link>
          <router-link v-if="isAdmin" to="/admin">管理后台</router-link>
        </nav>

        <div class="app-user">
          <template v-if="user">
            <img v-if="user.avatar" class="avatar" :src="user.avatar" alt="" />
            <el-dropdown @command="onCommand">
              <span class="user-name" style="cursor: pointer">{{ user.nickname }}</span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="mine">我的发布</el-dropdown-item>
                  <el-dropdown-item v-if="isAdmin" command="admin">管理后台</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return { user: null, isAdmin: false }
  },
  watch: {
    $route() {
      this.refreshUser()
    }
  },
  created() {
    this.refreshUser()
  },
  methods: {
    refreshUser() {
      const raw = localStorage.getItem('user')
      this.user = raw ? JSON.parse(raw) : null
      this.isAdmin = !!this.user && this.user.role === 'ADMIN'
    },
    onCommand(command) {
      if (command === 'logout') {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        this.refreshUser()
        this.$message.success('已退出登录')
        this.$router.push('/login')
        return
      }
      this.$router.push(command === 'admin' ? '/admin' : '/mine')
    }
  }
}
</script>
