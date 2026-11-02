<script setup lang="ts">
/** 骨架屏：加载占位 */
withDefaults(
  defineProps<{
    variant?: 'text' | 'title' | 'avatar' | 'card' | 'block'
    lines?: number
    width?: string
    height?: string
    round?: boolean
  }>(),
  { variant: 'text', lines: 3, height: '16px' }
)
</script>

<template>
  <div class="sk-wrap">
    <template v-if="variant === 'text'">
      <div
        v-for="i in lines"
        :key="i"
        class="sk line"
        :style="{ width: i === lines ? '62%' : '100%', animationDelay: `${i * 90}ms` }"
      />
    </template>
    <div
      v-else-if="variant === 'title'"
      class="sk line"
      :style="{ width: '46%', height: '22px', borderRadius: '7px' }"
    />
    <div v-else-if="variant === 'avatar'" class="sk" style="width: 44px; height: 44px; border-radius: 12px" />
    <div
      v-else-if="variant === 'card'"
      class="sk block"
      :style="{ width: '100%', height: height }"
    />
    <div v-else class="sk block" :style="{ width, height }" />
  </div>
</template>

<style scoped>
.sk-wrap {
  display: flex;
  flex-direction: column;
  gap: 9px;
  width: 100%;
}

.sk {
  background: linear-gradient(90deg, #eef0f8 25%, #f8f9fd 45%, #eef0f8 65%);
  background-size: 220% 100%;
  animation: shift 1.5s linear infinite;
  border-radius: 6px;
}

.line {
  height: 13px;
}

.block {
  border-radius: var(--r-md);
  min-height: 40px;
}

@keyframes shift {
  from {
    background-position: 220% 0;
  }
  to {
    background-position: -20% 0;
  }
}
</style>
