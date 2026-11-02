<script setup lang="ts">
/** 空状态占位 */
import AppIcon from './AppIcon.vue'

withDefaults(defineProps<{ title?: string; desc?: string; icon?: string; size?: number }>(), {
  title: '这里还什么都没有',
  icon: 'file',
  size: 108
})
</script>

<template>
  <div class="empty">
    <div class="art">
      <span class="ring r1" aria-hidden="true" />
      <span class="ring r2" aria-hidden="true" />
      <span class="ico"><AppIcon :name="icon" :size="30" color="#8b8fb5" /></span>
    </div>
    <h4>{{ title }}</h4>
    <p v-if="desc">{{ desc }}</p>
    <div class="slot"><slot /></div>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 40px 20px;
  text-align: center;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
}

.art {
  position: relative;
  width: 96px;
  height: 96px;
  display: grid;
  place-items: center;
  margin-bottom: 6px;
}

.ring {
  position: absolute;
  border-radius: 50%;
  border: 1.5px dashed var(--line-strong);
}
.r1 {
  inset: 0;
  animation: spin 22s linear infinite;
}
.r2 {
  inset: 14px;
  border-style: solid;
  border-color: rgba(99, 102, 241, 0.18);
  animation: spin 16s linear infinite reverse;
}

.ico {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: var(--surface-3);
  border: 1px solid var(--line);
  animation: float-y 4s var(--ease-in-out) infinite;
}

h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-2);
}
p {
  font-size: 12.5px;
  color: var(--text-4);
  max-width: 320px;
}

.slot {
  margin-top: 10px;
}
</style>
