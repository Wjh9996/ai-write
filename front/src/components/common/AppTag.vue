<script setup lang="ts">
/** 状态/语义标签：自动映射颜色，带柔和底色 */
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    label?: string
    tone?:
      | 'brand'
      | 'purple'
      | 'pink'
      | 'cyan'
      | 'teal'
      | 'green'
      | 'amber'
      | 'orange'
      | 'rose'
      | 'gray'
      | 'blue'
      | 'ocean'
    size?: 'xs' | 'sm' | 'md'
    dot?: boolean
    icon?: string
    outline?: boolean
  }>(),
  { tone: 'brand', size: 'sm' }
)

const GRAD: Record<string, string> = {
  brand: '99,102,241',
  purple: '168,85,247',
  pink: '236,72,153',
  cyan: '6,182,212',
  teal: '20,184,166',
  green: '34,197,94',
  amber: '245,158,11',
  orange: '249,115,22',
  rose: '244,63,94',
  gray: '122,125,153',
  blue: '59,130,246',
  ocean: '14,165,233'
}

const rgb = computed(() => GRAD[props.tone] ?? GRAD.brand)
</script>

<template>
  <span
    class="tag"
    :class="[`sz-${size}`, { 'is-outline': outline }]"
    :style="{ '--tone': rgb }"
  >
    <i v-if="dot" class="dot" aria-hidden="true" />
    <AppIcon v-else-if="icon" :name="icon" :size="size === 'xs' ? 11 : 13" />
    <slot>{{ label }}</slot>
  </span>
</template>

<style scoped>
.tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border-radius: var(--r-full);
  font-weight: 600;
  color: rgb(var(--tone));
  background: rgba(var(--tone), 0.11);
  border: 1px solid rgba(var(--tone), 0.2);
  line-height: 1;
  white-space: nowrap;
  transition: all var(--dur-fast) var(--ease-out);
}

.tag.is-outline {
  background: transparent;
}

.sz-xs {
  padding: 2px 7px;
  font-size: 10.5px;
}
.sz-sm {
  padding: 3px 9px;
  font-size: 11.5px;
}
.sz-md {
  padding: 5px 12px;
  font-size: 12.5px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgb(var(--tone));
  box-shadow: 0 0 0 3px rgba(var(--tone), 0.18);
  animation: pulse-dot 2.4s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%,
  100% {
    box-shadow: 0 0 0 3px rgba(var(--tone), 0.18);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(var(--tone), 0.08);
  }
}
</style>
