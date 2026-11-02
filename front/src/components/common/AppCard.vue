<script setup lang="ts">
/** 卡片容器：可选渐变标题条、悬浮抬升、光晕 */
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    title?: string
    desc?: string
    icon?: string
    tone?: 'none' | 'brand' | 'pink' | 'mint' | 'sunset' | 'violet' | 'ocean' | 'amber'
    hoverable?: boolean
    padding?: 'none' | 'sm' | 'md' | 'lg'
    flat?: boolean
  }>(),
  { tone: 'none', padding: 'md' }
)

const GRAD: Record<string, string> = {
  none: 'transparent',
  brand: 'var(--grad-brand)',
  pink: 'var(--grad-brand-soft)',
  mint: 'var(--grad-mint)',
  sunset: 'var(--grad-sunset)',
  violet: 'var(--grad-violet)',
  ocean: 'var(--grad-ocean)',
  amber: 'var(--grad-amber)'
}
</script>

<template>
  <section class="card" :class="[{ 'is-hover': hoverable, 'is-flat': flat }, `p-${padding}`]">
    <span class="glow" aria-hidden="true" />
    <div v-if="tone !== 'none'" class="top-bar" :style="{ background: GRAD[tone] }" aria-hidden="true" />
    <header v-if="title || $slots.header || $slots.actions" class="head">
      <div class="head-left">
        <span v-if="icon" class="head-icon"><AppIcon :name="icon" :size="16" /></span>
        <div class="head-text">
          <h3 v-if="title">{{ title }}</h3>
          <p v-if="desc">{{ desc }}</p>
          <slot name="header" />
        </div>
      </div>
      <div v-if="$slots.actions" class="head-actions"><slot name="actions" /></div>
    </header>
    <div class="body"><slot /></div>
    <footer v-if="$slots.footer" class="foot"><slot name="footer" /></footer>
  </section>
</template>

<style scoped>
.card {
  position: relative;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--r-lg);
  box-shadow: var(--sh-sm);
  overflow: hidden;
  transition: transform var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out),
    border-color var(--dur-base) var(--ease-out);
}

.card.is-hover:hover {
  transform: translateY(-3px);
  box-shadow: var(--sh-lg);
  border-color: var(--line-2);
}

.card.is-flat {
  box-shadow: none;
  background: var(--surface-2);
}

.glow {
  position: absolute;
  inset: -40% 55% 60% -40%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.08), transparent 70%);
  pointer-events: none;
  transition: opacity var(--dur-base) linear;
  opacity: 0;
}
.card.is-hover:hover .glow {
  opacity: 1;
}

.top-bar {
  height: 3px;
  width: 100%;
}

.p-none .body {
  padding: 0;
}
.p-sm .body {
  padding: 12px;
}
.p-md .body {
  padding: 16px;
}
.p-lg .body {
  padding: 22px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 15px 16px 0;
}
.p-sm .head {
  padding: 12px 12px 0;
}
.p-lg .head {
  padding: 20px 22px 0;
}

.head-left {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  min-width: 0;
}

.head-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--grad-brand);
  box-shadow: var(--sh-brand);
  flex: none;
}

.head-text h3 {
  font-size: 14.5px;
  font-weight: 700;
  letter-spacing: 0.2px;
}
.head-text p {
  font-size: 12.5px;
  color: var(--text-3);
  margin-top: 2px;
}

.head-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex: none;
}

.foot {
  padding: 12px 16px;
  border-top: 1px solid var(--line);
  background: var(--surface-2);
}
</style>
