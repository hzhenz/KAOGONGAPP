<script setup>
import { onMounted } from 'vue'
import { useAppState } from '../appState'

const {
  adminStats,
  userList,
  adminRecentRecords,
  newJobForm,
  jobResults,
  loading,
  updateUserStatus,
  createJob,
  startEditJob,
  editingJobId,
  editingJobForm,
  cancelEditJob,
  saveEditJob,
  deleteJob,
  loadAdminOverview,
  searchJobs,
  pageMessage,
} = useAppState()

onMounted(async () => {
  if (!userList.value.length) {
    await loadAdminOverview()
  }
  if (!jobResults.value.length) {
    await searchJobs()
  }
})

async function handleUserStatus(username, status) {
  try {
    await updateUserStatus(username, status)
    pageMessage.value = `用户 ${username} 已更新为 ${status}`
  } catch (error) {
    pageMessage.value = `更新用户状态失败：${error.message}`
  }
}

async function handleCreateJob() {
  try {
    await createJob()
    pageMessage.value = '岗位已新增。'
  } catch (error) {
    pageMessage.value = `新增岗位失败：${error.message}`
  }
}

async function handleSaveJob(id) {
  try {
    await saveEditJob(id)
    pageMessage.value = '岗位已更新。'
  } catch (error) {
    pageMessage.value = `更新岗位失败：${error.message}`
  }
}

async function handleDeleteJob(id) {
  try {
    await deleteJob(id)
    pageMessage.value = '岗位已删除。'
  } catch (error) {
    pageMessage.value = `删除岗位失败：${error.message}`
  }
}
</script>

<template>
  <section class="card">
    <h3>后台概览</h3>
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
              <button class="ghost mini-btn" @click="handleUserStatus(user.username, 'active')">启用</button>
              <button class="ghost mini-btn danger-btn" @click="handleUserStatus(user.username, 'blocked')">封禁</button>
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
          <button class="primary" @click="handleCreateJob">新增岗位</button>
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
                <button class="primary mini-btn" @click="handleSaveJob(job.id)">保存</button>
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
                  <button class="ghost mini-btn danger-btn" @click="handleDeleteJob(job.id)">删除</button>
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
            <p>学习小结：{{ record.summary }}</p>
            <small>{{ record.reviewedAt }}</small>
          </div>
        </div>
      </div>
    </section>
  </section>
</template>
