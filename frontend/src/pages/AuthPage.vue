<script setup>
import { useRouter } from 'vue-router'
import { useAppState } from '../appState'

const router = useRouter()
const { authMode, authForm, loginAccount, registerAccount, pageMessage } = useAppState()

async function submitLogin() {
  try {
    await loginAccount()
    pageMessage.value = '欢迎回来'
    router.push('/home')
  } catch (error) {
    pageMessage.value = `登录失败：${error.message}`
  }
}

async function submitRegister() {
  try {
    await registerAccount()
    pageMessage.value = '注册成功'
    router.push('/home')
  } catch (error) {
    pageMessage.value = `注册失败：${error.message}`
  }
}
</script>

<template>
  <section class="card auth-card auth-page-card">
    <div class="card-head">
      <h3>登录注册 + JWT</h3>
      <span class="status-chip">欢迎进入系统</span>
    </div>

    <div class="grid two-cols">
      <div>
        <div class="segmented auth-switch">
          <button :class="{ active: authMode === 'login' }" @click="authMode = 'login'">登录</button>
          <button :class="{ active: authMode === 'register' }" @click="authMode = 'register'">注册</button>
        </div>
        <div class="form-row">
          <label>用户名</label>
          <input v-model="authForm.username" placeholder="3-24位，建议英文或数字" />
        </div>
        <div class="form-row">
          <label>密码</label>
          <input v-model="authForm.password" type="password" placeholder="至少6位" />
        </div>
        <div v-if="authMode === 'register'" class="form-row">
          <label>昵称</label>
          <input v-model="authForm.displayName" placeholder="例如：考公冲刺生" />
        </div>
        <button v-if="authMode === 'login'" class="primary" @click="submitLogin">登录并进入系统</button>
        <button v-else class="primary" @click="submitRegister">注册并进入系统</button>
      </div>
      <div class="result-box">
        <p>演示账号（默认内置）：</p>
        <p>student / 123456</p>
        <p>admin / admin123</p>
        <p>被管理员封禁后将无法继续登录。</p>
      </div>
    </div>
  </section>
</template>
