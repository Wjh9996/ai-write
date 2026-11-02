<script setup lang="ts">
/** 全局 Toast 宿主 */
import { useUiStore } from '@/stores/ui'
import AppIcon from './AppIcon.vue'

const ui = useUiStore()

const ICON: Record<string, string> = {
  success: 'check',
  error: 'warning',
  info: 'info',
  warn: 'warning'
}
</script>

<template>
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="toast-host">
      <div v-for="t in ui.toasts" :key="t.id" class="toast" :class="t.type">
        <span class="ico"><AppIcon :name="ICON[t.type]" :size="15" color="#fff" /></span>
        <div class="txt">
          <p class="ti">{{ t.title }}</p>
          <p v-if="t.desc" class="de">{{ t.desc }}</p>
        </div>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 18px;
  right: 18px;
  z-index: 2000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 250px;
  max-width: 360px;
  padding: 12px 14px;
  border-radius: var(--r-md);
  background: var(--surface);
  box-shadow: var(--sh-lg);
  border: 1px solid var(--line);
  position: relative;
  overflow: hidden;
  pointer-events: auto;
}

.toast::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  height: 2px;
  background: var(--grad-brand);
  animation: countdown 3.2s linear forwards;
}

@keyframes countdown {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
}

.ico {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  flex: none;
}
.success .ico {
  background: linear-gradient(135deg, #34d399, #10b981);
}
.error .ico {
  background: linear-gradient(135deg, #fb7185, #ef4444);
}
.warn .ico {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
}
.info .ico {
  background: linear-gradient(135deg, #60a5fa, #3b82f6);
}

.ti {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-1);
}
.de {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 1px;
}
</style>
