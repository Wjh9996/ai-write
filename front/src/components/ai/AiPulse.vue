<script setup lang="ts">
/** AI 呼吸指示器：生成中的视觉焦点 */
withDefaults(defineProps<{ active?: boolean; size?: number; label?: string }>(), { size: 40, active: true })
</script>

<template>
  <div class="pulse" :class="{ on: active }">
    <span class="ring o1" aria-hidden="true" />
    <span class="ring o2" aria-hidden="true" />
    <span class="core">
      <span class="bars" aria-hidden="true">
        <i v-for="n in 4" :key="n" :style="{ animationDelay: `${n * 120}ms` }" />
      </span>
    </span>
    <span v-if="label" class="lb">{{ label }}</span>
  </div>
</template>

<style scoped>
.pulse {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pulse > .core {
  position: relative;
  width: v-bind('size + "px"');
  height: v-bind('size + "px"');
  border-radius: 50%;
  background: var(--grad-brand);
  display: grid;
  place-items: center;
  box-shadow: var(--sh-brand);
}

.ring {
  position: absolute;
  border-radius: 50%;
  inset: -6px;
}
.pulse.on .o1 {
  animation: pulse-ring 1.9s ease-out infinite;
}
.pulse.on .o2 {
  animation: pulse-ring-pink 1.9s ease-out 0.6s infinite;
}
.pulse:not(.on) .core {
  background: var(--line-2);
  box-shadow: none;
}
.pulse:not(.on) .bars i {
  animation: none;
  transform: scaleY(0.4);
  background: var(--text-4);
}

.bars {
  display: flex;
  align-items: center;
  gap: 3px;
  height: 40%;
}
.bars i {
  width: 3px;
  height: 100%;
  border-radius: 2px;
  background: #fff;
  transform-origin: center;
  animation: wave-bar 1s var(--ease-in-out) infinite;
}

.lb {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--brand-strong);
}
</style>
