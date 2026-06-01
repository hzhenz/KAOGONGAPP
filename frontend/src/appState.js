import { computed, reactive, ref } from 'vue'

const API_BASE = 'http://localhost:8080/api'
const TOKEN_KEY = 'kaogongapp_token'
const MAX_FOCUS_SECONDS = 180 * 60
const MIN_FOCUS_SECONDS = 5

const auth = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  username: '',
  displayName: '',
  role: '',
})

const authMode = ref('login')
const authForm = reactive({
  username: '',
  password: '',
  displayName: '',
})

const focusOptions = ['行测-数量关系', '行测-判断推理', '申论-热点素材', '申论-写作框架']
const focusForm = reactive({
  subject: focusOptions[0],
  durationSeconds: 45 * 60,
  mode: '学习小结',
})

const timer = reactive({
  running: false,
  secondsLeft: focusForm.durationSeconds,
  totalSeconds: focusForm.durationSeconds,
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
  comment: '完成专注后提交学习小结，系统将进行反向验收。',
  submitDeadline: '',
  minTextLength: 50,
  maxFileSize: 0,
  failureReason: '',
})

const heatmapDays = ref([])
const essayMaterials = ref([])
const recentRecords = ref([])
const focusHistory = ref([])
const SUMMARIES_KEY = 'kaogongapp_summaries'
const summaries = ref(JSON.parse(localStorage.getItem(SUMMARIES_KEY) || '[]'))
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
  const hours = Math.floor(timer.secondsLeft / 3600)
  const minutes = Math.floor((timer.secondsLeft % 3600) / 60)
  const seconds = timer.secondsLeft % 60

  if (hours > 0) {
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
  }
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const isLoggedIn = computed(() => !!auth.token)
const isAdmin = computed(() => auth.role === 'admin')
const canSubmitEvidence = computed(() => !timer.running && !!timer.sessionId)
const submitTextLength = computed(() => submitText.value.trim().length)
const submitGuidance = computed(() => {
  if (timer.running) return '倒计时未结束：请等待专注结束后再提交学习小结。'
  if (!timer.sessionId) return '请先开始一次专注会话。'
  if (submitTextLength.value < result.minTextLength) {
    return `学习小结不足 ${result.minTextLength} 字：当前 ${submitTextLength.value} 字，请补充后提交。`
  }
  return '倒计时结束后，请提交不少于 50 字的学习小结，系统会保存每次小结记录。'
})
const pagedJobSummary = computed(() => `第 ${jobPage.page} / ${Math.max(jobPage.totalPages, 1)} 页，共 ${jobPage.total} 条`)
const focusDurationText = computed(() => formatDuration(focusForm.durationSeconds))

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

  if (response.status === 204) return null
  return response.json()
}

function saveToken(token) {
  auth.token = token
  localStorage.setItem(TOKEN_KEY, token)
}

function normalizeFocusDuration(seconds) {
  const numeric = Number(seconds)
  if (!Number.isFinite(numeric)) return MIN_FOCUS_SECONDS
  return Math.max(MIN_FOCUS_SECONDS, Math.min(MAX_FOCUS_SECONDS, Math.round(numeric)))
}

function formatDuration(totalSeconds) {
  const safeSeconds = normalizeFocusDuration(totalSeconds)
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const seconds = safeSeconds % 60
  const parts = []
  if (hours) parts.push(`${hours}小时`)
  if (minutes) parts.push(`${minutes}分钟`)
  if (seconds || !parts.length) parts.push(`${seconds}秒`)
  return parts.join(' ')
}

function parseErrorMessage(error) {
  const raw = error?.message || '请求失败'
  try {
    const parsed = JSON.parse(raw)
    return parsed.message || parsed.error || raw
  } catch {
    return raw
  }
}

function saveSummariesToStorage() {
  try {
    localStorage.setItem(SUMMARIES_KEY, JSON.stringify(summaries.value || []))
  } catch (e) {
    console.warn('保存学习小结到本地存储失败', e)
  }
}

function addSummary({ subject = '未命名', minutes = 0, summary = '' } = {}) {
  const record = { subject, minutes, summary, createdAt: new Date().toISOString() }
  summaries.value.unshift(record)
  saveSummariesToStorage()
}

function loadLocalSummaries() {
  try {
    summaries.value = JSON.parse(localStorage.getItem(SUMMARIES_KEY) || '[]')
  } catch {
    summaries.value = []
  }
}

function getSubmitFailureReason(message) {
  const normalized = parseErrorMessage({ message })
  if (normalized.includes('倒计时尚未结束')) return '倒计时未结束：请等待计时结束后再提交。'
  if (normalized.includes('提交超时')) return '已超时：倒计时结束后仅可在 5 分钟内补交。'
  if (normalized.includes('内容过于敷衍') || normalized.includes('学习小结不足')) return `学习小结不足 ${result.minTextLength} 字：请补充后重新提交。`
  if (normalized.includes('会话不存在')) return '会话已失效：请重新开始一次专注。'
  return normalized
}

function validateSubmitBeforeRequest() {
  if (!timer.sessionId) {
    return '请先开始一次专注会话。'
  }
  if (timer.running) {
    return '倒计时未结束：请等待计时结束后再提交。'
  }
  if (submitTextLength.value < result.minTextLength) {
    return `学习小结不足 ${result.minTextLength} 字：当前 ${submitTextLength.value} 字，请补充后重新提交。`
  }
  return ''
}

function resetSession() {
  clearInterval(tick)
  timer.running = false
  timer.secondsLeft = normalizeFocusDuration(focusForm.durationSeconds)
  timer.totalSeconds = normalizeFocusDuration(focusForm.durationSeconds)
  timer.sessionTitle = ''
  timer.sessionId = ''
  timer.serverStartedAt = ''
  submitState.value = '待提交'
  submitText.value = ''
  result.status = '待验收'
  result.confidence = '-'
  result.comment = '完成专注后提交学习小结，系统将进行反向验收。'
  result.submitDeadline = ''
  result.failureReason = ''
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
}

async function registerAccount() {
  pageMessage.value = ''
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
}

async function startFocus() {
  pageMessage.value = ''
  const data = await request('/focus/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      subject: focusForm.subject,
      totalSeconds: normalizeFocusDuration(focusForm.durationSeconds),
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
  result.comment = '倒计时结束后才能提交学习小结，结束后有 5 分钟补交窗口。'
  result.submitDeadline = ''
  result.failureReason = ''

  clearInterval(tick)
  tick = setInterval(() => {
    if (timer.secondsLeft <= 0) {
      clearInterval(tick)
      timer.running = false
      submitState.value = '待验收'
      result.status = '待验收'
      result.comment = '倒计时已结束，请在 5 分钟内提交学习小结。'
      return
    }
    timer.secondsLeft -= 1
  }, 1000)
}

async function submitEvidence() {
  const precheckMessage = validateSubmitBeforeRequest()
  if (precheckMessage) {
    result.failureReason = precheckMessage
    result.status = '验收失败'
    throw new Error(precheckMessage)
  }

  loading.submit = true
  submitState.value = '验收中'
  try {
    const data = await request('/focus/submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: timer.sessionId,
        subject: focusForm.subject,
        mode: focusForm.mode,
        summary: submitText.value,
      }),
    })

    result.status = data.status
    result.confidence = data.confidence
    result.comment = data.comment
    result.submitDeadline = data.submitDeadline
    result.minTextLength = data.minTextLength
    result.maxFileSize = data.maxFileSize
    result.failureReason = data.passed ? '' : getSubmitFailureReason(data.comment)
    submitState.value = data.passed ? '成功' : '失败'
    timer.sessionId = ''
    await loadOverview()
    await loadFocusHistory()
    if (isAdmin.value) await loadAdminOverview()
  } catch (error) {
    result.failureReason = getSubmitFailureReason(error.message)
    result.status = '验收失败'
    throw new Error(result.failureReason)
  } finally {
    loading.submit = false
  }
}

async function extractEssay() {
  if (!essayInput.value.trim()) {
    throw new Error('请先粘贴申论材料。')
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
  } finally {
    loading.adminAction = false
  }
}

async function deleteJob(id) {
  loading.adminAction = true
  try {
    await request(`/admin/jobs/${id}`, { method: 'DELETE' })
    if (editingJobId.value === id) cancelEditJob()
    await loadAdminOverview()
    await searchJobs()
  } finally {
    loading.adminAction = false
  }
}

function handleFileChange(event) {
  const file = event.target.files?.[0] || null
  submitFile.value = file
  submitFileName.value = file ? file.name : '未选择图片'
}

async function bootstrap() {
  if (!auth.token) return
  try {
    await loadMe()
    await initializeAppData()
  } catch {
    logout('登录已失效，请重新登录。')
  }
}

function disposeAppState() {
  clearInterval(tick)
}

export function useAppState() {
  return {
    auth,
    authMode,
    authForm,
    focusOptions,
    focusForm,
    timer,
    stats,
    adminStats,
    submitState,
    submitText,
    pageMessage,
    loading,
    result,
    heatmapDays,
    essayMaterials,
    recentRecords,
    focusHistory,
    summaries,
    userList,
    jobFilters,
    jobResults,
    jobPage,
    essayInput,
    essaySummary,
    adminRecentRecords,
    newJobForm,
    editingJobId,
    editingJobForm,
    progress,
    timeText,
    isLoggedIn,
    isAdmin,
    canSubmitEvidence,
    submitTextLength,
    submitGuidance,
    pagedJobSummary,
    focusDurationText,
    loginAccount,
    registerAccount,
    logout,
    loadOverview,
    loadFocusHistory,
    loadLocalSummaries,
    loadAdminOverview,
    searchJobs,
    changeJobPage,
    startFocus,
    submitEvidence,
    addSummary,
    extractEssay,
    updateUserStatus,
    createJob,
    startEditJob,
    cancelEditJob,
    saveEditJob,
    deleteJob,
    resetSession,
    bootstrap,
    disposeAppState,
  }
}
