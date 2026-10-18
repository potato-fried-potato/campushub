import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue')
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue')
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue')
  },
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
    path: '/publish',
    name: 'publish',
    component: () => import('../views/PublishView.vue')
  },
  {
    path: '/messages',
    name: 'messages',
    component: () => import('../views/MessagesView.vue')
  },
  {
    path: '/bargains',
    name: 'bargains',
    component: () => import('../views/BargainView.vue')
  },
  {
    path: '/reservations',
    name: 'reservations',
    component: () => import('../views/ReservationView.vue')
  },
  {
    path: '/credit',
    name: 'credit',
    component: () => import('../views/CreditView.vue')
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminView.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
