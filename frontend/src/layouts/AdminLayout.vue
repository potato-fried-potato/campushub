<template>
  <div class="admin-shell">
    <aside class="admin-side">
      <div class="admin-brand">
        <span class="brand-mark">智</span>
        <span>管理后台</span>
      </div>
      <nav>
        <router-link class="admin-nav" :to="adminPath('overview')">数据概览</router-link>
        <router-link class="admin-nav" :to="adminPath('users')">用户管理</router-link>
        <router-link class="admin-nav" :to="adminPath('items')">商品管理</router-link>
        <router-link class="admin-nav" :to="adminPath('reports')">举报处置</router-link>
      </nav>
      <div class="admin-foot">
        <router-link to="/">← 返回集市</router-link>
      </div>
    </aside>

    <section class="admin-main">
      <header class="admin-top">
        <span class="who">{{ user ? user.nickname : '管理员' }}（管理员）</span>
        <el-button link type="primary" @click="logout">退出登录</el-button>
      </header>
      <div class="admin-body">
        <router-view />
      </div>
    </section>
  </div>
</template>

<script>
import { auth, clearAuth } from '../auth-store'

export default {
  name: 'AdminLayout',
  computed: {
    user() {
      return auth.user
    }
  },
  methods: {
    adminPath(page) {
      return '/admin/' + page
    },
    logout() {
      clearAuth()
      this.$message.success('已退出登录')
      this.$router.push('/admin/login')
    }
  }
}
</script>

<style scoped>
.admin-shell { display: flex; min-height: 100vh; background: var(--bg); }
.admin-side {
  width: 208px; flex-shrink: 0; background: #fff; border-right: 1px solid var(--border);
  display: flex; flex-direction: column; padding: 16px 12px;
}
.admin-brand { display: flex; align-items: center; gap: 8px; font-weight: 700; padding: 4px 8px 16px; }
.admin-nav {
  display: block; padding: 9px 12px; border-radius: 6px; color: var(--text-sub);
  font-size: 14px; margin-bottom: 4px;
}
.admin-nav:hover { background: #f1f4f9; color: var(--text); }
.admin-nav.router-link-active { background: var(--brand-soft); color: var(--brand); font-weight: 600; }
.admin-foot { margin-top: auto; padding: 8px 12px; font-size: 13px; color: var(--text-muted); }
.admin-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.admin-top {
  height: 56px; background: #fff; border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: flex-end; gap: 16px; padding: 0 20px;
}
.admin-top .who { font-size: 14px; color: var(--text-sub); }
.admin-body { padding: 20px; }
</style>
