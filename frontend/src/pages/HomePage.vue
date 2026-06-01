<script setup>
import { onMounted, ref } from 'vue'
import { useAppState } from '../appState'

const { stats, heatmapDays, recentRecords, focusHistory, summaries, addSummary, loading, loadOverview, loadFocusHistory, loadLocalSummaries, focusForm } = useAppState()

const newSummary = ref('')

function saveSummary() {
  const text = newSummary.value.trim()
  if (!text) return
  addSummary({ subject: focusForm.subject || '自定义', minutes: Math.round((focusForm.durationSeconds || 0) / 60), summary: text })
  newSummary.value = ''
}

onMounted(async () => {
  if (!recentRecords.value.length) await loadOverview()
  if (!focusHistory.value.length) await loadFocusHistory()
  loadLocalSummaries()
})
</script>

<template>
  <section class="hero card">
    <div>
      <span class="badge">需求规格说明书 2.0 落地版</span>
      <h2>从“记录时间”升级为“校验成果”</h2>
      <p>支持账号体系、专注打卡、申论提纯、岗位检索，以及基于学习小结的持续记录与回顾。</p>
    </div>
    <div class="hero-stats">
      <div>
        <strong>{{ stats.todayFocusMinutes }} 分钟</strong>
        <span>今日有效时长</span>
      </div>
      <div>
        <strong>{{ stats.completedSessions }}</strong>
        <span>完成场次</span>
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

  <section class="grid two-cols">
    <div class="card">
      <h3>极简热力图</h3>
      <div class="heatmap">
        <span v-for="(day, index) in heatmapDays" :key="index" :class="`lv-${day}`"></span>
      </div>
    </div>

    <div class="card">
      <h3>最近打卡记录</h3>
      <div v-if="loading.dashboard" class="empty-tip">概览加载中...</div>
      <div v-else-if="!recentRecords.length" class="empty-tip">暂无最近记录</div>
      <div v-for="record in recentRecords" :key="`${record.subject}-${record.reviewedAt}`" class="list-item">
        <strong>{{ record.subject }}</strong>
        <p>{{ record.minutes }} 分钟 · {{ record.passed ? '通过' : '未通过' }}</p>
        <p>学习小结：{{ record.summary }}</p>
        <small>{{ record.reviewedAt }}</small>
      </div>
    </div>
  </section>

  <section class="card">
    <h3>学习小结历史</h3>
    <div class="submit-summary">
      <textarea v-model="newSummary" rows="4" placeholder="填写学习小结（将记录为每次小结）"></textarea>
      <div style="margin-top:8px;"><button class="btn" @click="saveSummary">保存学习小结</button></div>
    </div>
    <div v-if="loading.history" class="empty-tip">历史记录加载中...</div>
    <div v-else-if="!focusHistory.length && !summaries.length" class="empty-tip">暂无学习小结记录</div>
    <div v-for="record in focusHistory" :key="`${record.subject}-${record.reviewedAt}-${record.mode}`" class="list-item">
      <strong>{{ record.subject }} · {{ record.minutes }} 分钟</strong>
      <p>{{ record.mode }} · {{ record.passed ? '通过' : '未通过' }} · 置信度 {{ record.confidence }}</p>
      <p>评语：{{ record.comment }}</p>
      <p>学习小结：{{ record.summary || '未填写学习小结' }}</p>
      <small>{{ record.reviewedAt }}</small>
    </div>
    <div v-if="summaries.length" style="margin-top:12px">
      <h4>本地学习小结</h4>
      <div v-for="(s, idx) in summaries" :key="s.createdAt + idx" class="list-item">
        <strong>{{ s.subject }} · {{ s.minutes }} 分钟</strong>
        <p>学习小结：{{ s.summary }}</p>
        <small>{{ new Date(s.createdAt).toLocaleString() }}</small>
      </div>
    </div>
  </section>
</template>
