<script setup lang="ts">
/**
 * 按钮：支持 5 种变体 + 渐变主按钮 + 加载态 + 图标插槽
 */
import AppIcon from './AppIcon.vue'

const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'ghost' | 'soft' | 'outline' | 'danger' | 'text'
    size?: 'sm' | 'md' | 'lg'
    icon?: string
    loading?: boolean
    disabled?: boolean
    block?: boolean
    round?: boolean
  }>(),
  { variant: 'soft', size: 'md' }
)

const emit = defineEmits<{ click: [MouseEvent] }>()

function onClick(e: MouseEvent) {
  if (props.disabled || props.loading) return
  emit('click', e)
}
</script>

<template>
  <button
    class="btn"
    :class="[`v-${variant}`, `s-${size}`, { 'is-block': block, 'is-round': round, 'is-disabled': disabled }]"
    :disabled="disabled || loading"
    type="button"
    @click="onClick"
  >
    <span class="btn-bg" aria-hidden="true" />
    <span class="btn-body">
      <template v-if="loading">
        <span class="spin" aria-hidden="true" />
      </template>
      <AppIcon v-else-if="icon" :name="icon" :size="size === 'sm' ? 14 : size === 'lg' ? 19 : 16" />
      <slot />
    </span>
  </button>
</template>

<style scoped>
.btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: var(--r-md);
  font-weight: 600;
  letter-spacing: 0.2px;
  white-space: nowrap;
  overflow: hidden;
  transition: transform var(--dur-fast) var(--ease-out), box-shadow var(--dur-fast) var(--ease-out),
    opacity var(--dur-fast) linear, background var(--dur-fast) linear, color var(--dur-fast) linear;
  user-select: none;
}

.btn-body {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn:active:not(.is-disabled) {
  transform: scale(0.96);
}

.btn.is-block {
  width: 100%;
}

.btn.is-round {
  border-radius: var(--r-full);
}

.btn.is-disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 尺寸 */
.s-sm {
  height: 30px;
  padding: 0 11px;
  font-size: 12.5px;
}
.s-md {
  height: 36px;
  padding: 0 15px;
  font-size: 13.5px;
}
.s-lg {
  height: 44px;
  padding: 0 22px;
  font-size: 15px;
  border-radius: var(--r-lg);
}

/* 主按钮：渐变 + 光扫 */
.v-primary {
  color: #fff;
  background: var(--grad-brand);
  background-size: 180% 100%;
  box-shadow: var(--sh-brand);
  animation: gradient-pan 6s ease-in-out infinite;
}
.v-primary:hover:not(.is-disabled) {
  box-shadow: 0 14px 32px -10px rgba(99, 102, 241, 0.6);
  transform: translateY(-1px);
}
.v-primary .btn-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, transparent 30%, rgba(255, 255, 255, 0.36) 50%, transparent 70%);
  width: 40%;
  transform: translateX(-120%);
  transition: transform 620ms var(--ease-out);
}
.v-primary:hover .btn-bg::after {
  transform: translateX(300%);
}

/* 柔和底 */
.v-soft {
  background: var(--surface-3);
  color: var(--text-1);
  box-shadow: var(--sh-xs);
}
.v-soft:hover:not(.is-disabled) {
  background: #eaedf9;
  color: var(--brand-strong);
}

/* 幽灵/次要 */
.v-ghost {
  background: transparent;
  color: var(--text-2);
}
.v-ghost:hover:not(.is-disabled) {
  background: rgba(99, 102, 241, 0.08);
  color: var(--brand-strong);
}

/* 描边 */
.v-outline {
  background: var(--surface);
  border: 1px solid var(--line-2);
  color: var(--text-1);
}
.v-outline:hover:not(.is-disabled) {
  border-color: var(--brand);
  color: var(--brand-strong);
  box-shadow: 0 4px 14px -6px rgba(99, 102, 241, 0.5);
}

/* 危险 */
.v-danger {
  background: linear-gradient(135deg, #f97316, #ef4444);
  color: #fff;
  box-shadow: 0 8px 22px -8px rgba(239, 68, 68, 0.55);
}
.v-danger:hover:not(.is-disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 28px -8px rgba(239, 68, 68, 0.7);
}

/* 纯文字 */
.v-text {
  background: transparent;
  color: var(--brand-strong);
  padding-inline: 6px;
}
.v-text:hover:not(.is-disabled) {
  background: rgba(99, 102, 241, 0.08);
}

.spin {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  animation: spin 700ms linear infinite;
}
.v-soft .spin,
.v-ghost .spin,
.v-outline .spin,
.v-text .spin {
  border-color: rgba(99, 102, 241, 0.25);
  border-top-color: var(--brand);
}
</style>
