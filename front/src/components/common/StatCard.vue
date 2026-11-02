<script setup lang="ts">
/** 统计卡片：数字滚动 + 渐变图标 + 迷你趋势条 */
import { computed, onMounted, ref, watch } from 'vue'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: number
    suffix?: string
    icon?: string
    tone?: 'brand' | 'pink' | 'mint' | 'sunset' | 'violet' | 'ocean' | 'amber'
    hint?: string
    progress?: number
    delay?: number
  }>(),
  { tone: 'brand', delay: 0 }
)

const GRAD: Record<string, string> = {
  brand: 'var(--grad-brand)',
  pink: 'var(--grad-brand-soft)',
  mint: 'var(--grad-mint)',
  sunset: 'var(--grad-sunset)',
  violet: 'var(--grad-violet)',
  ocean: 'var(--grad-ocean)',
  amber: 'var(--grad-amber)'
}

const shown = ref(0)

function animate() {
  const target = props.value
  const start = performance.now()
  const dur = 900
  const step = (t: number) => {
    const p = Math.min(1, (t - start) / dur)
    const eased = 1 - Math.pow(1 - p, 3)
    shown.value = Math.round(target * eased)
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

watch(() => props.value, animate)
onMounted(() => setTimeout(animate, props.delay))

const display = computed(() => shown.value.toLocaleString('zh-CN'))
</script>

<template>
  <div class="stat" :style="{ animationDelay: `${delay}ms` }">
    <span class="blob" :style="{ background: GRAD[tone] }" aria-hidden="true" />
    <div class="ico" :style="{ background: GRAD[tone] }">
      <AppIcon :name="icon ?? 'bolt'" :size="17" color="#fff" />
    </div>
    <div class="meta">
      <p class="lb">{{ label }}</p>
      <p class="val mono">
        {{ display }}<em v-if="suffix">{{ suffix }}</em>
      </p>
      <p v-if="hint" class="hint">{{ hint }}</p>
      <div v-if="progress !== undefined" class="pb">
        <i :style="{ width: `${Math.min(100, progress)}%`, background: GRAD[tone] }" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.stat {
  position: relative;
  overflow: hidden;
  display: flex;
  gap: 13px;
  align-items: flex-start;
  padding: 15px 16px;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-sm);
  animation: fade-up var(--dur-slow) var(--ease-out) both;
  transition: transform var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out);
}
.stat:hover {
  transform: translateY(-3px);
  box-shadow: var(--sh-md);
}

.blob {
  position: absolute;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  filter: blur(34px);
  opacity: 0.16;
  top: -34px;
  right: -26px;
  pointer-events: none;
  animation: float-slow 9s var(--ease-in-out) infinite;
}

.ico {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  flex: none;
  box-shadow: var(--sh-sm);
  transition: transform var(--dur-base) var(--ease-spring);
}
.stat:hover .ico {
  transform: rotate(-8deg) scale(1.07);
}

.meta {
  min-width: 0;
  flex: 1;
}

.lb {
  font-size: 12px;
  color: var(--text-3);
  font-weight: 600;
}

.val {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.4px;
  line-height: 1.3;
  color: var(--text-1);
}
.val em {
  font-style: normal;
  font-size: 12.5px;
  color: var(--text-3);
  margin-left: 3px;
  font-weight: 600;
}

.hint {
  font-size: 11.5px;
  color: var(--text-4);
  margin-top: 1px;
}

.pb {
  margin-top: 7px;
  height: 4px;
  border-radius: var(--r-full);
  background: var(--surface-3);
  overflow: hidden;
}
.pb i {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width 900ms var(--ease-out);
}
</style>
