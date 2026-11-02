<script setup lang="ts">
import { useUiStore } from '@/stores/ui'
import ToastHost from '@/components/common/ToastHost.vue'

const ui = useUiStore()
</script>

<template>
  <router-view v-slot="{ Component }">
    <Transition name="view" mode="out-in">
      <component :is="Component" />
    </Transition>
  </router-view>

  <ToastHost />

  <Teleport to="body">
    <Transition name="modal">
      <div v-if="ui.globalLoading" class="global-mask">
        <div class="loader">
          <span class="orbit o1" aria-hidden="true" />
          <span class="orbit o2" aria-hidden="true" />
          <span class="orbit o3" aria-hidden="true" />
          <span class="core" aria-hidden="true" />
        </div>
        <p class="txt">{{ ui.loadingText }}</p>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.global-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  background: rgba(246, 247, 251, 0.72);
  backdrop-filter: blur(8px);
}

.loader {
  position: relative;
  width: 76px;
  height: 76px;
  display: grid;
  place-items: center;
}

.orbit {
  position: absolute;
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: var(--brand);
  animation: spin 1.1s linear infinite;
}
.o1 {
  inset: 0;
}
.o2 {
  inset: 10px;
  border-top-color: var(--purple);
  animation-duration: 1.5s;
  animation-direction: reverse;
}
.o3 {
  inset: 20px;
  border-top-color: var(--pink);
  animation-duration: 0.9s;
}

.core {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--grad-brand);
  animation: float-y 1.4s var(--ease-in-out) infinite;
}

.txt {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  letter-spacing: 0.4px;
}
</style>
