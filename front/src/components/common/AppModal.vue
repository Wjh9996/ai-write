<script setup lang="ts">
/** 弹窗：带渐变标题头、毛玻璃遮罩、pop 入场 */
import AppIcon from './AppIcon.vue'

withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    desc?: string
    icon?: string
    width?: number
    closable?: boolean
  }>(),
  { width: 520, closable: true }
)

const emit = defineEmits<{ 'update:modelValue': [boolean] }>()

function close() {
  emit('update:modelValue', false)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="mask" @click.self="close()">
        <div class="modal-panel" :style="{ width: `${width}px` }">
          <header class="hd">
            <div class="hd-left">
              <span v-if="icon" class="ico"><AppIcon :name="icon" :size="17" /></span>
              <div>
                <h3>{{ title }}</h3>
                <p v-if="desc">{{ desc }}</p>
              </div>
            </div>
            <button v-if="closable" class="x" aria-label="关闭" @click="close()">
              <AppIcon name="close" :size="16" />
            </button>
          </header>
          <div class="bd"><slot /></div>
          <footer v-if="$slots.footer" class="ft"><slot name="footer" /></footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 900;
  display: grid;
  place-items: center;
  background: rgba(26, 28, 46, 0.36);
  backdrop-filter: blur(6px);
  padding: 20px;
}

.modal-panel {
  max-width: 100%;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border-radius: var(--r-xl);
  box-shadow: var(--sh-lg);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.hd {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--line);
  background: linear-gradient(180deg, #fbfaff, var(--surface));
}

.hd-left {
  display: flex;
  gap: 11px;
  align-items: flex-start;
}

.ico {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--grad-brand);
  box-shadow: var(--sh-brand);
  flex: none;
}

.hd h3 {
  font-size: 15.5px;
  font-weight: 700;
}
.hd p {
  font-size: 12.5px;
  color: var(--text-3);
  margin-top: 2px;
}

.x {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--text-3);
  transition: all var(--dur-fast) var(--ease-out);
}
.x:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--c-danger);
  transform: rotate(90deg);
}

.bd {
  padding: 18px 20px;
  overflow: auto;
  flex: 1;
}

.ft {
  padding: 14px 20px;
  border-top: 1px solid var(--line);
  background: var(--surface-2);
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
