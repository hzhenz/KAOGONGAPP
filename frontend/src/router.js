import { createRouter, createWebHistory } from 'vue-router'
import { useAppState } from './appState'
import AuthPage from './pages/AuthPage.vue'
import HomePage from './pages/HomePage.vue'
import FocusPage from './pages/FocusPage.vue'
import EssayPage from './pages/EssayPage.vue'
import JobsPage from './pages/JobsPage.vue'
import AdminPage from './pages/AdminPage.vue'

const { auth } = useAppState()

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/auth', name: 'auth', component: AuthPage, meta: { guestOnly: true } },
  { path: '/home', name: 'home', component: HomePage, meta: { requiresAuth: true } },
  { path: '/focus', name: 'focus', component: FocusPage, meta: { requiresAuth: true } },
  { path: '/essay', name: 'essay', component: EssayPage, meta: { requiresAuth: true } },
  { path: '/jobs', name: 'jobs', component: JobsPage, meta: { requiresAuth: true } },
  { path: '/admin', name: 'admin', component: AdminPage, meta: { requiresAuth: true, requiresAdmin: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const isLoggedIn = !!auth.token
  const isAdmin = auth.role === 'admin'

  if (to.meta.requiresAuth && !isLoggedIn) {
    return '/auth'
  }
  if (to.meta.guestOnly && isLoggedIn) {
    return '/home'
  }
  if (to.meta.requiresAdmin && !isAdmin) {
    return '/home'
  }
  return true
})

export default router
