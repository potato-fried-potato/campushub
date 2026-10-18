<template>
  <router-view v-if="isStandaloneRoute" />

  <div v-else class="app-shell">
    <!-- 页头：品牌 + 搜索 + 常用操作 -->
    <header class="ch-header">
      <div class="ch-header-inner">
        <router-link to="/" class="ch-brand">
          <span class="ch-brand-mark">智</span>
          <span class="ch-brand-text">
            <span class="ch-brand-name">智校互联</span>
            <span class="ch-brand-sub">郑州轻工业大学闲置集市</span>
          </span>
        </router-link>

        <div class="ch-search">
          <div class="ch-search-box">
            <input
              v-model="keyword"
              class="ch-search-input"
              placeholder="搜索教材、数码、自行车、宿舍好物"
              @keyup.enter="search"
            />
            <button class="ch-search-btn" @click="search">搜索</button>
          </div>
        </div>

        <div class="ch-actions">
          <div v-if="user && !isAdmin" class="ch-trade-entry" @click="$router.push('/trade')">
            交易中心
            <span class="cnt" v-if="badgeTotal > 0">{{ badgeTotal }}</span>
          </div>
          <button v-if="user && !isAdmin" class="ch-primary-btn" @click="openPublishDialog">+ 发布闲置</button>

          <el-dropdown v-if="user" @command="onCommand">
            <span class="ch-user-chip">
              <img v-if="user.avatar" class="avatar" :src="user.avatar" alt="" />
              <span class="name">{{ user.nickname }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="!isAdmin" command="purchases">我的购买</el-dropdown-item>
                <el-dropdown-item v-if="!isAdmin" command="mine">我的发布</el-dropdown-item>
                <el-dropdown-item v-if="!isAdmin" command="trade">交易中心</el-dropdown-item>
                <el-dropdown-item v-if="!isAdmin" command="credit">信用中心</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin">管理后台</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <template v-else>
            <span class="ch-login-link" @click="openAuthDialog('login')">登录</span>
            <button class="ch-primary-btn" @click="openAuthDialog('register')">注册</button>
          </template>
        </div>
      </div>
    </header>

    <!-- 页面导航 -->
    <nav class="ch-nav">
      <div class="ch-nav-inner">
        <router-link to="/" class="ch-nav-link" :class="{ active: $route.path === '/' && !$route.query.categoryId }">
          首页
        </router-link>
        <template v-if="user && !isAdmin">
          <router-link to="/mine" class="ch-nav-link">我的发布</router-link>
          <router-link to="/purchases" class="ch-nav-link">我的购买</router-link>
          <router-link to="/trade" class="ch-nav-link">交易中心</router-link>
          <router-link to="/credit" class="ch-nav-link">信用中心</router-link>
        </template>
        <router-link v-if="isAdmin" :to="adminHome" class="ch-nav-link">管理后台</router-link>

        <div class="ch-nav-right">
          <span
            class="ch-nav-link"
            :class="{ active: !currentCampus }"
            @click="switchCampus('')"
          >全部校区</span>
          <span
            class="ch-nav-link"
            :class="{ active: currentCampus === 'DONGFENG' }"
            @click="switchCampus('DONGFENG')"
          >东风校区</span>
          <span
            class="ch-nav-link"
            :class="{ active: currentCampus === 'KEXUE' }"
            @click="switchCampus('KEXUE')"
          >科学校区</span>
        </div>
      </div>
    </nav>

    <main class="app-main">
      <router-view />
    </main>
  </div>

  <template v-if="!isStandaloneRoute">
    <AuthDialog @success="refreshUser" />
    <PublishDialog />
    <DealChatDialog />
  </template>
</template>

<script>
import AuthDialog from './components/AuthDialog.vue'
import PublishDialog from './components/PublishDialog.vue'
import DealChatDialog from './components/DealChatDialog.vue'
import { authDialog, openAuth } from './auth-dialog'
import { publishDialog, openPublish } from './publish-dialog'
import { auth, clearAuth } from './auth-store'
import axios from 'axios'
import { dealChat } from './deal-chat'

export default {
  name: 'App',
  components: { AuthDialog, PublishDialog, DealChatDialog },
  data() {
    return {
      badges: { bargain: 0, reservation: 0, message: 0, deal: 0, chat: 0 },
      keyword: '',
      timer: null
    }
  },
  computed: {
    // 管理端（含独立的管理员登录页）不套用户端外壳
    isStandaloneRoute() {
      return this.$route.matched.some(r => r.meta && (r.meta.admin || r.meta.bare))
    },
    user() {
      return auth.user
    },
    isAdmin() {
      return !!auth.user && auth.user.role === 'ADMIN'
    },
    badgeTotal() {
      return this.badges.bargain + this.badges.reservation + this.badges.message + this.badges.deal + this.badges.chat
    },
    currentCampus() {
      return this.$route.query.campus || ''
    },
    adminHome() {
      return '/admin/overview'
    }
  },
  watch: {
    user(u) {
      if (u) this.loadBadges()
      else this.badges = { bargain: 0, reservation: 0, message: 0, deal: 0, chat: 0 }
    },
    $route() {
      this.checkAuthQuery()
      this.keyword = this.$route.query.keyword || ''
      this.loadBadges()
      // 进入管理端时收起用户端弹窗，避免跨端残留
      if (this.isStandaloneRoute) {
        authDialog.visible = false
        publishDialog.visible = false
        dealChat.visible = false
      }
    }
  },
  created() {
    this.checkAuthQuery()
    this.keyword = this.$route.query.keyword || ''
    this.loadBadges()
    // 交易单私聊已读后即时刷新角标
    dealChat.onRead = this.loadBadges
    // 停留期间定期刷新交易待办角标
    this.timer = setInterval(() => this.loadBadges(), 60000)
  },
  beforeUnmount() {
    clearInterval(this.timer)
  },
  methods: {
    // 交易中心待办角标：待成交交易单 + 待处理出价 + 待处理预约 + 未读留言
    loadBadges() {
      if (!auth.user) return
      Promise.all([
        axios.get('/api/bargains', { params: { view: 'received' } }),
        axios.get('/api/reservations', { params: { view: 'received' } }),
        axios.get('/api/messages/mine', { params: { view: 'received' } }),
        axios.get('/api/deals', { params: { view: 'received' } })
      ]).then(([b, r, m, d]) => {
        this.badges = {
          bargain: b.data.code === 0 ? b.data.data.pendingCount : 0,
          reservation: r.data.code === 0 ? r.data.data.pendingCount : 0,
          message: m.data.code === 0 ? m.data.data.totalUnread : 0,
          deal: d.data.code === 0 ? d.data.data.pendingCount : 0,
          chat: d.data.code === 0 ? (d.data.data.unreadMessageCount || 0) : 0
        }
      }).catch(() => {})
    },
    search() {
      const kw = this.keyword.trim()
      if (this.$route.path === '/' && (this.$route.query.keyword || '') === kw) {
        window.dispatchEvent(new CustomEvent('campushub:search'))
        return
      }
      this.$router.push({ path: '/', query: kw ? { keyword: kw } : {} }).catch(() => {})
    },
    switchCampus(campus) {
      this.$router.push({ path: '/', query: campus ? { campus } : {} }).catch(() => {})
    },
    // 兼容旧的 /login、/register 链接
    checkAuthQuery() {
      const tab = this.$route.query.auth
      if (tab === 'login' || tab === 'register') {
        openAuth(tab)
      }
      if (this.$route.query.publish === '1') {
        openPublish()
      }
    },
    openAuthDialog(tab) {
      openAuth(tab)
    },
    openPublishDialog() {
      openPublish()
    },
    // 登录态由 auth-store 响应式提供，仅保留回调占位
    refreshUser() {},
    onCommand(command) {
      if (command === 'logout') {
        const wasAdmin = this.isAdmin
        clearAuth()
        this.$message.success('已退出登录')
        this.$router.push(wasAdmin ? '/admin/login' : '/')
        return
      }
      const paths = { admin: '/admin/overview', mine: '/mine', purchases: '/purchases', trade: '/trade', credit: '/credit' }
      this.$router.push(paths[command] || '/')
    }
  }
}
</script>
