<script setup lang="ts">
/** 进度条 / 环形进度二合一 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    value: number
    max?: number
    type?: 'bar' | 'ring'
    size?: number
    thickness?: number
    tone?: string
    striped?: boolean
    showLabel?: boolean
    height?: number
  }>(),
  { max: 100, type: 'bar', size: 56, thickness: 5, tone: '99,102,241', height: 8 }
)

const pct = computed(() => Math.max(0, Math.min(100, Math.round((props.value / props.max) * 100))))
const dash = computed(() => {
  const r = (props.size - props.thickness) / 2
  const c = 2 * Math.PI * r
  return { c, offset: c * (1 - pct.value / 100) }
})
</script>

<template>
  <div class="pg" :class="type">
    <!-- 条形 -->
    <div
      v-if="type === 'bar'"
      class="track"
      :style="{ height: `${height}px`, '--tone': tone }"
    >
      <div class="fill" :style="{ width: `${pct}%` }">
        <span v-if="striped" class="stripe" aria-hidden="true" />
      </div>
    </div>

    <!-- 环形 -->
    <div v-else class="ring" :style="{ width: `${size}px`, height: `${size}px`, '--tone': tone }">
      <svg :width="size" :height="size">
        <circle
          :cx="size / 2"
          :cy="size / 2"
          :r="(size - thickness) / 2"
          fill="none"
          stroke="rgba(var(--tone),0.15)"
          :stroke-width="thickness"
        />
        <circle
          class="prog"
          :cx="size / 2"
          :cy="size / 2"
          :r="(size - thickness) / 2"
          fill="none"
          stroke="rgb(var(--tone))"
          :stroke-width="thickness"
          stroke-linecap="round"
          :stroke-dasharray="dash.c"
          :stroke-dashoffset="dash.offset"
          transform="rotate(-90)"
          :style="{ transformOrigin: 'center' }"
        />
      </svg>
      <span v-if="showLabel" class="lb mono">{{ pct }}%</span>
      <div v-else class="slot"><slot /></div>
    </div>
  </div>
</template>

<style scoped>
.pg {
  display: inline-flex;
  align-items: center;
}

.bar {
  width: 100%;
}

.track {
  width: 100%;
  background: rgba(var(--tone), 0.12);
  border-radius: var(--r-full);
  overflow: hidden;
  position: relative;
}

.fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, rgba(var(--tone), 0.85), rgb(var(--tone)));
  transition: width 420ms var(--ease-out);
  position: relative;
  overflow: hidden;
}

.stripe {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    100deg,
    transparent 20%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 80%
  );
  background-size: 220% 100%;
  animation: shimmer 1.4s linear infinite;
}

.ring {
  position: relative;
  display: grid;
  place-items: center;
}

.prog {
  transition: stroke-dashoffset 520ms var(--ease-out), stroke var(--dur-base) linear;
}

.lb {
  position: absolute;
  font-size: 12px;
  font-weight: 700;
  color: rgb(var(--tone));
}

.slot {
  position: absolute;
  display: grid;
  place-items: center;
}

@keyframes shimmer {
  from {
    background-position: 220% 0;
  }
  to {
    background-position: -20% 0;
  }
}
</style>
