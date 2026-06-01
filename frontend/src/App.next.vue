<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

const API_BASE = 'http://localhost:8080/api'
const TOKEN_KEY = 'kaogongapp_token'
const navItems = ['首页', '专注打卡', '申论提纯', '岗位检索', '后台概览']
const activeNav = ref('首页')
const focusOptions = ['行测-数量关系', '行测-判断推理', '申论-热点素材', '申论-写作框架']

const authMode = ref('login')
const auth = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  username: '',
  displayName: '',
  role: '',
})
const authForm = reactive({
  username: '',
  password: '',
  displayName: '',
})

const focusForm = reactive({
  subject: focusOptions[0],
  minutes: 45,
  mode: '文本提交',
})

const timer = reactive({
  running: false,
  secondsLeft: focusForm.minutes * 60,
  totalSeconds: focusForm.minutes * 60,
  sessionTitle: '',
  sessionId: '',
  serverStartedAt: '',
})

const stats = reactive({
  todayFocusMinutes: 0,
  completedSessions: 0,
  aiPassRate: '-',
  jobsCount: 0,
})

const adminStats = reactive({
  totalUsers: 0,
  activeUsers: 0,
  totalJobs: 0,
  recentPassCount: 0,
  totalFocusRecords: 0,
})

const submitState = ref('待提交')
const submitText = ref('')
const submitFileName = ref('未选择图片')
const submitFile = ref(null)
const pageMessage = ref('')
const loading = reactive({
  dashboard: false,
  jobs: false,
  submit: false,
  essay: false,
  admin: false,
  adminAction: false,
  history: false,
})

const result = reactive({
  status: '待验收',
  confidence: '-',
  comment: '完成专注后提交学习成果，系统将调用 AI 进行反向验收。',
  submitDeadline: '',
  minTextLength: 50,
  maxFileSize: 5 * 1024 * 1024,
})

const heatmapDays = ref([])
const essayMaterials = ref([])
const recentRecords = ref([])
const focusHistory = ref([])
const userList = ref([])
const jobFilters = reactive({ year: '2024', province: '广东', major: '法学', page: '1', size: '6' })
const jobResults = ref([])
const jobPage = reactive({ total: 0, page: 1, size: 6, totalPages: 0 })
const essayInput = ref('')
const essaySummary = ref('将政策材料、时评段落或申论原文粘贴到此处，系统将自动提取核心论点与可背诵表达。')
const adminRecentRecords = ref([])
const newJobForm = reactive({
  name: '',
  ratio: '',
  score: '',
  tag: '',
  province: '',
  major: '',
  year: '2025',
})
const editingJobId = ref(null)
const editingJobForm = reactive({
  name: '',
  ratio: '',
  score: '',
  tag: '',
  province: '',
  major: '',
  year: '',
})

let tick = null

const progress = computed(() => {
  if (!timer.totalSeconds) return 0
  return Math.max(0, Math.min(100, ((timer.totalSeconds - timer.secondsLeft) / timer.totalSeconds) * 100))
})

const timeText = computed(() => {
  const m = String(Math.floor(timer.secondsLeft / 60)).padStart(2, '0')
  const s = String(timer.secondsLeft % 60).padStart(2, '0')
  return `${m}:${s}`
})

const isLoggedIn = computed(() => !!auth.token)
const isAdmin = computed(() => auth.role === 'admin')
const canSubmitEvidence = computed(() => !timer.running && !!timer.sessionId)
const maxFileSizeText = computed(() => `${Math.round(result.maxFileSize / 1024 / 1024)}MB`)
const pagedJobSummary = computed(() => `第 ${jobPage.page} / ${Math.max(jobPage.totalPages, 1)} 页，共 ${jobPage.total} 条`)

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {})
  if (auth.token) {
    headers.set('Authorization', `Bearer ${auth.token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  if (!response.ok) {
    const message = await response.text()
    if (response.status === 401) {
      logout('登录已过期，请重新登录。')
    }
    throw new Error(message || '请求失败')
  }

  if (response.status === 204) {
    return null
  }
  return response.json()
}

function saveToken(token) {
  auth.token = token
  localStorage.setItem(TOKEN_KEY, token)
}

function logout(message = '已退出登录。') {
  auth.token = ''
  auth.username = ''
  auth.displayName = ''
  auth.role = ''
  localStorage.removeItem(TOKEN_KEY)
  resetSession()
  pageMessage.value = message
}

async function loadMe() {
  const data = await request('/auth/me')
  auth.username = data.username
  auth.displayName = data.displayName
  auth.role = data.role
}

async function initializeAppData() {
  await loadOverview()
  await loadFocusHistory()
  await searchJobs()
  if (isAdmin.value) {
    await loadAdminOverview()
  }
}

async function loginAccount() {
  pageMessage.value = ''
  try {
    const data = await request('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: authForm.username,
        password: authForm.password,
      }),
    })
    saveToken(data.token)
    await loadMe()
    await initializeAppData()
    pageMessage.value = `欢迎回来，${auth.displayName}`
  } catch (error) {
    pageMessage.value = `登录失败：${error.message}`
  }
}

async function registerAccount() {
  pageMessage.value = ''
  try {
    const data = await request('/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: authForm.username,
        password: authForm.password,
        displayName: authForm.displayName,
      }),
    })
    saveToken(data.token)
    await loadMe()
    await initializeAppData()
    pageMessage.value = `注册成功，欢迎你，${auth.displayName}`
  } catch (error) {
    pageMessage.value = `注册失败：${error.message}`
  }
}

async function loadOverview() {
  loading.dashboard = true
  try {
    const data = await request('/overview')
    Object.assign(stats, data.stats)
    heatmapDays.value = data.heatmapDays || []
    essayMaterials.value = data.essayMaterials || []
    recentRecords.value = data.recentRecords || []
  } catch (error) {
    pageMessage.value = `概览加载失败：${error.message}`
  } finally {
    loading.dashboard = false
  }
}

async function loadFocusHistory() {
  loading.history = true
  try {
    const data = await request('/focus/history')
    focusHistory.value = data.records || []
  } catch (error) {
    pageMessage.value = `历史记录加载失败：${error.message}`
  } finally {
    loading.history = false
  }
}

async function loadAdminOverview() {
  if (!isAdmin.value) return
  loading.admin = true
  try {
    const data = await request('/admin/overview')
    Object.assign(adminStats, data.stats)
    userList.value = data.users || []
    adminRecentRecords.value = data.recentRecords || []
  } catch (error) {
    pageMessage.value = `后台概览加载失败：${error.message}`
  } finally {
    loading.admin = false
  }
}

async function searchJobs() {
  loading.jobs = true
  try {
    const query = new URLSearchParams(jobFilters).toString()
    const data = await request(`/jobs?${query}`)
    jobResults.value = data.items || []
    jobPage.total = data.total || 0
    jobPage.page = data.page || 1
    jobPage.size = data.size || 6
    jobPage.totalPages = data.totalPages || 0
  } catch (error) {
    pageMessage.value = `岗位检索失败：${error.message}`
  } finally {
    loading.jobs = false
  }
}

function changeJobPage(nextPage) {
  const target = Math.max(1, Math.min(nextPage, Math.max(jobPage.totalPages, 1)))
  jobFilters.page = String(target)
  searchJobs()
}

async function startFocus() {
  pageMessage.value = ''
  try {
    const data = await request('/focus/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        subject: focusForm.subject,
        minutes: Number(focusForm.minutes),
        mode: focusForm.mode,
      }),
    })

    timer.running = true
    timer.totalSeconds = data.totalSeconds
    timer.secondsLeft = data.totalSeconds
    timer.sessionTitle = data.subject
    timer.sessionId = data.sessionId
    timer.serverStartedAt = data.startedAt
    submitState.value = data.status
    result.status = '等待提交'
    result.confidence = '-'
    result.comment = '倒计时结束后才能提交，结束后有 5 分钟补交窗口。'
    result.submitDeadline = ''

    clearInterval(tick)
    tick = setInterval(() => {
      if (timer.secondsLeft <= 0) {
        clearInterval(tick)
        timer.running = false
        submitState.value = '待验收'
        result.status = '待验收'
        result.comment = '倒计时已结束，请在 5 分钟内提交图文或文本成果。'
        return
      }
      timer.secondsLeft -= 1
    }, 1000)
  } catch (error) {
    pageMessage.value = `开始专注失败：${error.message}`
  }
}

async function submitEvidence() {
  if (!timer.sessionId) {
    pageMessage.value = '请先开始一次专注会话。'
    return
  }
  if (timer.running) {
    pageMessage.value = '倒计时尚未结束，暂不能提交验收。'
    return
  }

  loading.submit = true
  submitState.value = '验收中'
  try {
    const formData = new FormData()
    formData.append('sessionId', timer.sessionId)
    formData.append('subject', focusForm.subject)
    formData.append('mode', focusForm.mode)
    formData.append('text', submitText.value)
    if (submitFile.value) {
      formData.append('file', submitFile.value)
    }

    const data = await request('/focus/submit', {
      method: 'POST',
      body: formData,
    })

    result.status = data.status
    result.confidence = data.confidence
    result.comment = data.comment
    result.submitDeadline = data.submitDeadline
    result.minTextLength = data.minTextLength
    result.maxFileSize = data.maxFileSize
    submitState.value = data.passed ? '成功' : '失败'
    timer.sessionId = ''
    await loadOverview()
    await loadFocusHistory()
    if (isAdmin.value) {
      await loadAdminOverview()
    }
  } catch (error) {
    pageMessage.value = `提交验收失败：${error.message}`
    submitState.value = '失败'
  } finally {
    loading.submit = false
  }
}

async function extractEssay() {
  if (!essayInput.value.trim()) {
    pageMessage.value = '请先粘贴申论材料。'
    return
  }
  loading.essay = true
  try {
    const data = await request('/essay/extract', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: essayInput.value, sourceUrl: '' }),
    })
    essaySummary.value = data.summary
    essayMaterials.value = data.highlights || []
    pageMessage.value = '已完成申论素材提纯。'
  } catch (error) {
    pageMessage.value = `申论提纯失败：${error.message}`
  } finally {
    loading.essay = false
  }
}

async function updateUserStatus(username, status) {
  loading.adminAction = true
  try {
    await request(`/admin/users/${username}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    })
    await loadAdminOverview()
    pageMessage.value = `用户 ${username} 已更新为 ${status}`
  } catch (error) {
    pageMessage.value = `更新用户状态失败：${error.message}`
  } finally {
    loading.adminAction = false
  }
}

async function createJob() {
  loading.adminAction = true
  try {
    await request('/admin/jobs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(newJobForm),
    })
    resetNewJobForm()
    await loadAdminOverview()
    await searchJobs()
    pageMessage.value = '岗位已新增。'
  } catch (error) {
    pageMessage.value = `新增岗位失败：${error.message}`
  } finally {
    loading.adminAction = false
  }
}

function startEditJob(job) {
  editingJobId.value = job.id
  editingJobForm.name = job.name
  editingJobForm.ratio = job.ratio
  editingJobForm.score = job.score
  editingJobForm.tag = job.tag
  editingJobForm.province = job.province
  editingJobForm.major = job.major
  editingJobForm.year = job.year
}

function cancelEditJob() {
  editingJobId.value = null
  editingJobForm.name = ''
  editingJobForm.ratio = ''
  editingJobForm.score = ''
  editingJobForm.tag = ''
  editingJobForm.province = ''
  editingJobForm.major = ''
  editingJobForm.year = ''
}

async function saveEditJob(id) {
  loading.adminAction = true
  try {
    await request(`/admin/jobs/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(editingJobForm),
    })
    cancelEditJob()
    await loadAdminOverview()
    await searchJobs()
    pageMessage.value = '岗位已更新。'
  } catch (error) {
    pageMessage.value = `更新岗位失败：${error.message}`
  } finally {
    loading.adminAction = false
  }
}

async function deleteJob(id) {
  loading.adminAction = true
  try {
    await request(`/admin/jobs/${id}`, {
      method: 'DELETE',
    })
    if (editingJobId.value === id) {
      cancelEditJob()
    }
    await loadAdminOverview()
    await searchJobs()
    pageMessage.value = '岗位已删除。'
  } catch (error) {
    pageMessage.value = `删除岗位失败：${error.message}`
  } finally {
    loading.adminAction = false
  }
}

function resetNewJobForm() {
  newJobForm.name = ''
  newJobForm.ratio = ''
  newJobForm.score = ''
  newJobForm.tag = ''
  newJobForm.province = ''
  newJobForm.major = ''
  newJobForm.year = '2025'
}

function handleFileChange(event) {
  const file = event.target.files?.[0] || null
  submitFile.value = file
  submitFileName.value = file ? file.name : '未选择图片'
}

function resetSession() {
  clearInterval(tick)
  timer.running = false
  timer.secondsLeft = Number(focusForm.minutes) * 60
  timer.totalSeconds = Number(focusForm.minutes) * 60
  timer.sessionTitle = ''
  timer.sessionId = ''
  timer.serverStartedAt = ''
  submitState.value = '待提交'
  submitText.value = ''
  submitFile.value = null
  submitFileName.value = '未选择图片'
  result.status = '待验收'
  result.confidence = '-'
  result.comment = '完成专注后提交学习成果，系统将调用 AI 进行反向验收。'
  result.submitDeadline = ''
}

onMounted(async () => {
  if (!auth.token) return
  try {
    await loadMe()
    await initializeAppData()
  } catch {
    logout('登录已失效，请重新登录。')
  }
})

onBeforeUnmount(() => clearInterval(tick))
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

      <nav>
        <button
          v-for="item in navItems"
          :key="item"
          :class="['nav-item', { active: activeNav === item }]"
          @click="activeNav = item"
        >
          {{ item }}
        </button>
      </nav>

      <div class="side-note">
        <p>功能闭环</p>
        <strong>番茄钟 → 成果提交 → AI验收 → 热力图更新</strong>
      </div>
    </aside>

    <main class="main">
      <section class="hero card">
        <div>
          <span class="badge">需求规格说明书 2.0 落地版</span>
          <h2>从“记录时间”升级为“校验成果”</h2>
          <p>支持账号体系、专注打卡、申论提纯、岗位检索、管理员后台与零留存上传策略。</p>
        </div>
        <div class="hero-stats">
          <div>
            <strong>{{ timeText }}</strong>
            <span>本次专注时长</span>
          </div>
          <div>
            <strong>{{ submitState }}</strong>
            <span>打卡状态</span>
          </div>
        </div>
      </section>

      <section class="grid stats-grid">
        <div class="card stat-card">
          <span>今日有效时长</span>
          <strong>{{ stats.todayFocusMinutes }} 分钟</strong>
        </div>
        <div class="card stat-card">
          <span>完成场次</span>
          <strong>{{ stats.completedSessions }}</strong>
        </div>
        <div class="card stat-card">
          <span>AI 通过率</span>
          <strong>{{ stats.aiPassRate }}</strong>
        </div>
        <div class="card stat-card">
          <span>岗位总量</span>
          <strong>{{ stats.jobsCount }}</strong>
        </div>
      </section>

      <p v-if="pageMessage" class="page-message">{{ pageMessage }}</p>

      <section class="card auth-card">
        <div class="card-head">
          <h3>登录注册 + JWT</h3>
          <span class="status-chip" :class="{ running: isLoggedIn }">{{ isLoggedIn ? '已登录' : '未登录' }}</span>
        </div>

        <div v-if="!isLoggedIn" class="grid two-cols">
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
            <button v-if="authMode === 'login'" class="primary" @click="loginAccount">登录并进入系统</button>
            <button v-else class="primary" @click="registerAccount">注册并进入系统</button>
          </div>
          <div class="result-box">
            <p>演示账号（默认内置）：</p>
            <p>student / 123456</p>
            <p>admin / admin123</p>
            <p>被管理员封禁后将无法继续登录。</p>
          </div>
        </div>

        <div v-else class="result-box">
          <p>当前用户：{{ auth.displayName }}（{{ auth.username }}）</p>
          <p>角色：{{ auth.role }}</p>
          <button class="ghost" @click="logout()">退出登录</button>
        </div>
      </section>

      <section class="grid two-cols">
        <div class="card">
          <div class="card-head">
            <h3>专注打卡</h3>
            <span class="status-chip" :class="{ running: timer.running }">{{ timer.running ? '进行中' : '未开始' }}</span>
          </div>
          <div class="form-row">
            <label>专注学科</label>
            <select v-model="focusForm.subject">
              <option v-for="item in focusOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="form-row">
            <label>专注时长（分钟）</label>
            <input v-model="focusForm.minutes" type="number" min="15" max="180" />
          </div>
          <div class="form-row">
            <label>提交方式</label>
            <div class="segmented">
              <button :class="{ active: focusForm.mode === '文本提交' }" @click="focusForm.mode = '文本提交'">文本</button>
              <button :class="{ active: focusForm.mode === '图片提交' }" @click="focusForm.mode = '图片提交'">图片</button>
            </div>
          </div>
          <div class="actions">
            <button class="primary" @click="startFocus">开始专注</button>
            <button class="ghost" @click="resetSession">重置</button>
          </div>
          <div class="timer">
            <div class="timer-ring" :style="{ '--p': `${progress}%` }">
              <span>{{ timeText }}</span>
            </div>
            <div class="timer-meta">
              <p>专注主题：{{ timer.sessionTitle || '尚未开始' }}</p>
              <p>进度：{{ progress.toFixed(0) }}%</p>
              <progress :value="progress" max="100"></progress>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>AI 反向验收</h3>
            <span class="status-chip">{{ loading.submit ? '处理中' : (canSubmitEvidence ? '可提交' : '待倒计时结束') }}</span>
          </div>
          <div class="form-row">
            <label>文字成果（≥{{ result.minTextLength }}字）</label>
            <textarea v-model="submitText" rows="5" placeholder="请输入今日学习总结、错题反思或申论提炼内容"></textarea>
          </div>
          <div class="form-row">
            <label>图片成果（≤{{ maxFileSizeText }}）</label>
            <input type="file" accept="image/*" @change="handleFileChange" />
            <small>{{ submitFileName }}</small>
          </div>
          <button class="primary full" :disabled="loading.submit || !canSubmitEvidence" @click="submitEvidence">
            {{ loading.submit ? '验收中...' : '提交验收' }}
          </button>
          <div class="result-box">
            <p>状态：{{ result.status }}</p>
            <p>置信度：{{ result.confidence }}</p>
            <p>评语：{{ result.comment }}</p>
            <p v-if="result.submitDeadline">最晚提交：{{ result.submitDeadline }}</p>
          </div>
        </div>
      </section>

      <section class="grid three-cols top-align">
        <div class="card">
          <h3>极简热力图</h3>
          <div class="heatmap">
            <span v-for="(day, index) in heatmapDays" :key="index" :class="`lv-${day}`"></span>
          </div>
          <div class="sub-card-list">
            <div v-for="record in recentRecords" :key="`${record.subject}-${record.reviewedAt}`" class="list-item compact-item">
              <strong>{{ record.subject }}</strong>
              <p>{{ record.minutes }} 分钟 · {{ record.passed ? '通过' : '未通过' }}</p>
              <small>{{ record.reviewedAt }}</small>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>申论素材提纯</h3>
            <button class="ghost inline-btn" @click="extractEssay">{{ loading.essay ? '提纯中...' : '开始提纯' }}</button>
          </div>
          <div class="form-row">
            <label>粘贴政策材料 / 时评段落</label>
            <textarea v-model="essayInput" rows="7" placeholder="例如：基层治理、就业优先、数字政府、共同富裕等相关材料"></textarea>
          </div>
          <div class="result-box essay-summary-box">
            <p>{{ essaySummary }}</p>
          </div>
          <div v-for="item in essayMaterials" :key="item.title" class="list-item">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>

        <div class="card">
          <div class="card-head">
            <h3>岗位报录比检索</h3>
            <button class="ghost inline-btn" @click="searchJobs">筛选</button>
          </div>
          <div class="job-filter-grid">
            <input v-model="jobFilters.year" placeholder="年份" />
            <input v-model="jobFilters.province" placeholder="省份" />
            <input v-model="jobFilters.major" placeholder="专业" />
          </div>
          <div class="card-head pagination-head">
            <small>{{ pagedJobSummary }}</small>
            <div class="inline-actions">
              <button class="ghost mini-btn" :disabled="jobPage.page <= 1" @click="changeJobPage(jobPage.page - 1)">上一页</button>
              <button class="ghost mini-btn" :disabled="jobPage.page >= jobPage.totalPages" @click="changeJobPage(jobPage.page + 1)">下一页</button>
            </div>
          </div>
          <div v-if="loading.jobs" class="empty-tip">岗位数据加载中...</div>
          <div v-else-if="!jobResults.length" class="empty-tip">暂无匹配岗位</div>
          <div v-for="job in jobResults" :key="`${job.id}-${job.year}`" class="list-item row">
            <div>
              <strong>{{ job.name }}</strong>
              <p>{{ job.year }} · {{ job.province }} · {{ job.major }}</p>
              <p>{{ job.ratio }} · {{ job.score }}</p>
            </div>
            <span class="tag">{{ job.tag }}</span>
          </div>
        </div>
      </section>

      <section class="grid two-cols">
        <div class="card">
          <h3>历史打卡记录</h3>
          <div v-if="loading.history" class="empty-tip">历史记录加载中...</div>
          <div v-else-if="!focusHistory.length" class="empty-tip">暂无历史打卡记录</div>
          <div v-for="record in focusHistory" :key="`${record.subject}-${record.reviewedAt}-${record.mode}`" class="list-item">
            <strong>{{ record.subject }} · {{ record.minutes }} 分钟</strong>
            <p>{{ record.mode }} · {{ record.passed ? '通过' : '未通过' }} · 置信度 {{ record.confidence }}</p>
            <p>{{ record.comment }}</p>
            <small>{{ record.reviewedAt }}</small>
          </div>
        </div>

        <div class="card">
          <h3>需求映射</h3>
          <div class="list-item">
            <strong>FR1 账号体系</strong>
            <p>已实现注册、登录、JWT 鉴权、角色区分与管理员封禁用户。</p>
          </div>
          <div class="list-item">
            <strong>FR2 打卡校验逻辑</strong>
            <p>倒计时结束后才允许提交，5分钟超时自动作废，文本与图片双模式验收。</p>
          </div>
          <div class="list-item">
            <strong>FR3 岗位数据检索</strong>
            <p>支持分页筛选，并已补齐管理员新增、编辑、删除岗位能力。</p>
          </div>
          <div class="list-item">
            <strong>非功能补充</strong>
            <p>已预留真实 AI 接口参数，图片默认零留存，减少隐私残留。</p>
          </div>
        </div>
      </section>

      <section class="card">
        <h3>后台概览</h3>
        <div v-if="!isAdmin" class="empty-tip">当前登录角色不是管理员，后台管理能力已隐藏。</div>
        <template v-else>
          <div class="grid admin-stats-grid">
            <div class="list-item compact-item"><strong>{{ adminStats.totalUsers }}</strong><p>总用户数</p></div>
            <div class="list-item compact-item"><strong>{{ adminStats.activeUsers }}</strong><p>活跃用户</p></div>
            <div class="list-item compact-item"><strong>{{ adminStats.totalJobs }}</strong><p>岗位总数</p></div>
            <div class="list-item compact-item"><strong>{{ adminStats.totalFocusRecords }}</strong><p>打卡记录</p></div>
          </div>

          <section class="grid two-cols admin-section-grid">
            <div>
              <div class="list-block">
                <div class="card-head small-head">
                  <h4>用户状态管理</h4>
                  <span class="status-chip">{{ loading.adminAction ? '处理中' : '可操作' }}</span>
                </div>
                <div v-for="user in userList" :key="user.username" class="list-item row wrap-row">
                  <div>
                    <strong>{{ user.name }}</strong>
                    <p>{{ user.username }} · {{ user.role }}</p>
                  </div>
                  <div class="inline-actions">
                    <span class="tag" :class="user.status">{{ user.status }}</span>
                    <button class="ghost mini-btn" @click="updateUserStatus(user.username, 'active')">启用</button>
                    <button class="ghost mini-btn danger-btn" @click="updateUserStatus(user.username, 'blocked')">封禁</button>
                  </div>
                </div>
              </div>

              <div class="list-block">
                <div class="card-head small-head">
                  <h4>新增岗位</h4>
                </div>
                <div class="job-filter-grid admin-job-grid">
                  <input v-model="newJobForm.name" placeholder="岗位名称" />
                  <input v-model="newJobForm.ratio" placeholder="报录比，如 1:32" />
                  <input v-model="newJobForm.score" placeholder="进面分，如 123.5" />
                  <input v-model="newJobForm.tag" placeholder="标签" />
                  <input v-model="newJobForm.province" placeholder="省份" />
                  <input v-model="newJobForm.major" placeholder="专业" />
                  <input v-model="newJobForm.year" placeholder="年份" />
                </div>
                <button class="primary" @click="createJob">新增岗位</button>
              </div>
            </div>

            <div>
              <div class="list-block">
                <div class="card-head small-head">
                  <h4>岗位管理</h4>
                </div>
                <div v-for="job in jobResults" :key="`admin-${job.id}`" class="list-item">
                  <template v-if="editingJobId === job.id">
                    <div class="job-filter-grid admin-job-grid compact-grid">
                      <input v-model="editingJobForm.name" placeholder="岗位名称" />
                      <input v-model="editingJobForm.ratio" placeholder="报录比" />
                      <input v-model="editingJobForm.score" placeholder="进面分" />
                      <input v-model="editingJobForm.tag" placeholder="标签" />
                      <input v-model="editingJobForm.province" placeholder="省份" />
                      <input v-model="editingJobForm.major" placeholder="专业" />
                      <input v-model="editingJobForm.year" placeholder="年份" />
                    </div>
                    <div class="inline-actions top-gap">
                      <button class="primary mini-btn" @click="saveEditJob(job.id)">保存</button>
                      <button class="ghost mini-btn" @click="cancelEditJob">取消</button>
                    </div>
                  </template>
                  <template v-else>
                    <div class="row wrap-row">
                      <div>
                        <strong>{{ job.name }}</strong>
                        <p>{{ job.year }} · {{ job.province }} · {{ job.major }}</p>
                        <p>{{ job.ratio }} · {{ job.score }} · {{ job.tag }}</p>
                      </div>
                      <div class="inline-actions">
                        <button class="ghost mini-btn" @click="startEditJob(job)">编辑</button>
                        <button class="ghost mini-btn danger-btn" @click="deleteJob(job.id)">删除</button>
                      </div>
                    </div>
                  </template>
                </div>
              </div>

              <div class="list-block">
                <div class="card-head small-head">
                  <h4>最近审核记录</h4>
                </div>
                <div v-for="record in adminRecentRecords" :key="`${record.subject}-${record.reviewedAt}`" class="list-item">
                  <strong>{{ record.subject }} · {{ record.minutes }} 分钟</strong>
                  <p>{{ record.mode }} · 置信度 {{ record.confidence }} · {{ record.passed ? '通过' : '未通过' }}</p>
                  <small>{{ record.reviewedAt }}</small>
                </div>
              </div>
            </div>
          </section>
        </template>
      </section>
    </main>
  </div>
</template>
