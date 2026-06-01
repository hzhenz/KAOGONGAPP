<script setup>
import { useAppState } from '../appState'

const { essayInput, essaySummary, essayMaterials, loading, extractEssay, pageMessage } = useAppState()

async function handleExtract() {
  try {
    await extractEssay()
    pageMessage.value = '已完成申论素材提纯。'
  } catch (error) {
    pageMessage.value = `申论提纯失败：${error.message}`
  }
}
</script>

<template>
  <section class="card">
    <div class="card-head">
      <h3>申论素材提纯</h3>
      <button class="ghost inline-btn" @click="handleExtract">{{ loading.essay ? '提纯中...' : '开始提纯' }}</button>
    </div>
    <div class="form-row">
      <label>粘贴政策材料 / 时评段落</label>
      <textarea v-model="essayInput" rows="8" placeholder="例如：基层治理、就业优先、数字政府、共同富裕等相关材料"></textarea>
    </div>
    <div class="result-box essay-summary-box">
      <p>{{ essaySummary }}</p>
    </div>
    <div v-for="item in essayMaterials" :key="item.title" class="list-item">
      <strong>{{ item.title }}</strong>
      <p>{{ item.desc }}</p>
    </div>
  </section>
</template>
