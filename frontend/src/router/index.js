import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue')
  },
  { path: '/login', redirect: { path: '/', query: { auth: 'login' } } },
  { path: '/register', redirect: { path: '/', query: { auth: 'register' } } },
  {
    path: '/item/:id',
    name: 'itemDetail',
    component: () => import('../views/ItemDetailView.vue')
  },
  {
    path: '/mine',
    name: 'mine',
    component: () => import('../views/MyItemsView.vue')
  },
  {
    path: '/purchases',
    name: 'purchases',
    component: () => import('../views/MyPurchasesView.vue')
  },
  { path: '/publish', redirect: { path: '/', query: { publish: '1' } } },
  {
    path: '/trade',
    name: 'trade',
    component: () => import('../views/TradeCenterView.vue')
  },
  { path: '/messages', redirect: '/trade' },
  { path: '/bargains', redirect: '/trade' },
  { path: '/reservations', redirect: '/trade' },
  {
    path: '/credit',
    name: 'credit',
    component: () => import('../views/CreditView.vue')
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { admin: true },
    children: [
      { path: '', redirect: '/admin/overview' },
      { path: 'overview', name: 'adminOverview', component: () => import('../views/admin/AdminOverviewView.vue') },
      { path: 'users', name: 'adminUsers', component: () => import('../views/admin/AdminUsersView.vue') },
      { path: 'items', name: 'adminItems', component: () => import('../views/admin/AdminItemsView.vue') },
      { path: 'reports', name: 'adminReports', component: () => import('../views/admin/AdminReportsView.vue') }
    ]
  },
  {
    path: '/admin/login',
    name: 'adminLogin',
    component: () => import('../views/AdminLoginView.vue'),
    meta: { bare: true }
  },
  { path: '/:pathMatch(.*)*', name: 'notFound', component: () => import('../views/NotFoundView.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 管理端路由守卫：非管理员一律回集市
router.beforeEach((to, from, next) => {
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  const needAdmin = to.matched.some(r => r.meta && r.meta.admin)
  if (needAdmin) {
    if (!user || user.role !== 'ADMIN') {
      next('/admin/login')
      return
    }
  }
  // 管理员账号只做后台管理，用户侧功能页（我的发布 / 我的购买 / 交易中心 / 信用）一律回后台
  const userOnlyPages = ['/mine', '/purchases', '/trade', '/credit']
  if (user && user.role === 'ADMIN' && userOnlyPages.indexOf(to.path) >= 0) {
    next('/admin/overview')
    return
  }
  next()
})

export default router
