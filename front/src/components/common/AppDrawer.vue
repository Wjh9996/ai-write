<script setup lang="ts">
/** 抽屉：右侧滑出 */
import AppIcon from './AppIcon.vue'

withDefaults(defineProps<{ modelValue: boolean; title?: string; width?: number }>(), { width: 460 })
const emit = defineEmits<{ 'update:modelValue': [boolean] }>()
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="mask" @click.self="emit('update:modelValue', false)">
        <Transition name="drawer" appear>
          <aside class="drawer" :style="{ width: `${width}px` }">
            <header class="hd">
              <h3>{{ title }}</h3>
              <button class="x" @click="emit('update:modelValue', false)">
                <AppIcon name="close" :size="16" />
              </button>
            </header>
            <div class="bd"><slot /></div>
            <footer v-if="$slots.footer" class="ft"><slot name="footer" /></footer>
          </aside>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.mask {
  position: fixed;
  inset: 0;
  z-index: 880;
  background: rgba(26, 28, 46, 0.3);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: flex-end;
}

.drawer {
  height: 100%;
  max-width: 100%;
  background: var(--surface);
  display: flex;
  flex-direction: column;
  box-shadow: var(--sh-lg);
}

.hd {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--line);
}
.hd h3 {
  font-size: 15px;
  font-weight: 700;
}
.x {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--text-3);
}
.x:hover {
  background: var(--surface-3);
  color: var(--text-1);
  transform: rotate(90deg);
  transition: transform var(--dur-base) var(--ease-out);
}

.bd {
  flex: 1;
  overflow: auto;
  padding: 18px 20px;
}
.ft {
  padding: 14px 20px;
  border-top: 1px solid var(--line);
  background: var(--surface-2);
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
</style>
