<script setup>
import { onMounted } from 'vue'
import { useAppState } from '../appState'

const { jobFilters, jobResults, jobPage, pagedJobSummary, loading, searchJobs, changeJobPage } = useAppState()

onMounted(() => {
  if (!jobResults.value.length) {
    searchJobs()
  }
})
</script>

<template>
  <section class="card">
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
  </section>
</template>
