<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAppState } from './appState'

const route = useRoute()
const router = useRouter()
const { auth, isLoggedIn, isAdmin, logout, pageMessage } = useAppState()
const toastVisible = ref(false)
const toastText = ref('')
let toastTimer = null

const navItems = computed(() => {
  const items = [
    { label: '首页', to: '/home' },
    { label: '专注打卡', to: '/focus' },
    { label: '申论提纯', to: '/essay' },
    { label: '岗位检索', to: '/jobs' },
  ]
  if (isAdmin.value) {
    items.push({ label: '后台概览', to: '/admin' })
  }
  return items
})

function isActive(path) {
  return route.path === path
}

function handleLogout() {
  logout('已退出登录。')
  router.push('/auth')
}

function closeToast() {
  toastVisible.value = false
  pageMessage.value = ''
  if (toastTimer) {
    clearTimeout(toastTimer)
    toastTimer = null
  }
}

watch(pageMessage, (message) => {
  if (!message) {
    toastVisible.value = false
    return
  }

  toastText.value = message
  toastVisible.value = true

  if (toastTimer) {
    clearTimeout(toastTimer)
  }

  toastTimer = setTimeout(() => {
    closeToast()
  }, 3200)
})

onBeforeUnmount(() => {
  if (toastTimer) {
    clearTimeout(toastTimer)
  }
})
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">专</div>
        <div>
          <h1>专注者</h1>
          <p>考公版 · AI防作弊督学</p>
        </div>
      </div>

      <nav v-if="isLoggedIn">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          :class="['nav-item', { active: isActive(item.to) }]"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div v-if="isLoggedIn" class="result-box sidebar-user-box">
        <p>当前用户：{{ auth.displayName }}（{{ auth.username }}）</p>
        <p>角色：{{ auth.role }}</p>
        <button class="ghost" @click="handleLogout">退出登录</button>
      </div>

      <div class="side-note">
        <p>功能闭环</p>
        <strong>番茄钟 → 成果提交 → AI验收 → 热力图更新</strong>
      </div>
    </aside>

    <main class="main">
      <div v-if="toastVisible" class="toast-message" @click="closeToast">
        {{ toastText }}
      </div>
      <RouterView />
    </main>
  </div>
</template>
