<script setup>
import { onBeforeUnmount } from 'vue'
import { useAppState } from '../appState'

const {
  focusOptions,
  focusForm,
  timer,
  progress,
  timeText,
  loading,
  submitText,
  result,
  submitState,
  canSubmitEvidence,
  startFocus,
  submitEvidence,
  resetSession,
  pageMessage,
  focusDurationText,
  submitTextLength,
  submitGuidance,
} = useAppState()

async function handleStart() {
  try {
    await startFocus()
  } catch (error) {
    pageMessage.value = `开始专注失败：${error.message}`
  }
}

async function handleSubmit() {
  try {
    await submitEvidence()
  } catch (error) {
    pageMessage.value = `提交学习小结失败：${error.message}`
    submitState.value = '失败'
  }
}

onBeforeUnmount(() => {
  // 保持全局状态，不在路由切换时重置
})
</script>

<template>
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
        <label>专注时长（5秒 - 180分钟）</label>
        <input v-model="focusForm.durationSeconds" type="range" min="5" max="10800" step="1" />
        <small>当前时长：{{ focusDurationText }}</small>
      </div>
      <div class="form-row">
        <label>提交形式</label>
        <small>当前固定为：{{ focusForm.mode }}</small>
      </div>
      <div class="actions">
        <button class="primary" @click="handleStart">开始专注</button>
        <button class="ghost" @click="resetSession">重置</button>
      </div>
      <div class="timer">
        <div class="timer-ring" :style="{ '--p': `${progress}%` }">
          <span>{{ timeText }}</span>
        </div>
        <div class="timer-meta">
          <p>专注主题：{{ timer.sessionTitle || '尚未开始' }}</p>
          <p>当前设定：{{ focusDurationText }}</p>
          <p>进度：{{ progress.toFixed(0) }}%</p>
          <progress :value="progress" max="100"></progress>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-head">
        <h3>学习小结验收</h3>
        <span class="status-chip">{{ loading.submit ? '处理中' : (canSubmitEvidence ? '可提交' : '待倒计时结束') }}</span>
      </div>
      <div class="form-row">
        <label>学习小结（≥{{ result.minTextLength }}字）</label>
        <textarea v-model="submitText" rows="8" placeholder="请输入本次专注后的学习小结，例如：今天完成了哪些内容、遇到了什么问题、总结了哪些方法、下一次准备如何改进。"></textarea>
        <small>当前字数：{{ submitTextLength }}</small>
      </div>
      <div class="result-box">
        <p>提交提示：{{ submitGuidance }}</p>
        <p v-if="result.failureReason">失败原因：{{ result.failureReason }}</p>
      </div>
      <button class="primary full" :disabled="loading.submit || !canSubmitEvidence" @click="handleSubmit">
        {{ loading.submit ? '提交中...' : '提交学习小结' }}
      </button>
      <div class="result-box">
        <p>状态：{{ result.status }}</p>
        <p>置信度：{{ result.confidence }}</p>
        <p>评语：{{ result.comment }}</p>
        <p v-if="result.submitDeadline">最晚提交：{{ result.submitDeadline }}</p>
      </div>
    </div>
  </section>
</template>
