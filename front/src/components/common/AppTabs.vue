<script setup lang="ts">
/** Tab 切换：滑块指示器 */
import { computed, ref, watch, onMounted, nextTick } from 'vue'

interface Item {
  key: string
  label: string
  icon?: string
  badge?: string | number
}

const props = withDefaults(
  defineProps<{ modelValue: string; items: Item[]; size?: 'sm' | 'md'; block?: boolean }>(),
  { size: 'md' }
)
const emit = defineEmits<{ 'update:modelValue': [string] }>()

const wrap = ref<HTMLElement | null>(null)
const indicator = ref({ left: 0, width: 0 })

const activeIndex = computed(() => Math.max(0, props.items.findIndex((i) => i.key === props.modelValue)))

function moveIndicator() {
  if (!wrap.value) return
  const node = wrap.value.querySelectorAll<HTMLElement>('.tab-item')[activeIndex.value]
  if (!node) return
  indicator.value = { left: node.offsetLeft, width: node.offsetWidth }
}

watch(() => [props.modelValue, props.items.length], () => nextTick(moveIndicator))
onMounted(() => nextTick(moveIndicator))
</script>

<template>
  <div class="tabs" :class="[`sz-${size}`, { 'is-block': block }]" ref="wrap">
    <span
      class="indicator"
      :style="{ transform: `translateX(${indicator.left}px)`, width: `${indicator.width}px` }"
      aria-hidden="true"
    />
    <button
      v-for="(it, idx) in items"
      :key="it.key"
      class="tab-item"
      :class="{ on: modelValue === it.key }"
      type="button"
      @click="emit('update:modelValue', it.key)"
    >
      <span class="txt">{{ it.label }}</span>
      <span v-if="it.badge !== undefined" class="badge mono">{{ it.badge }}</span>
      <span class="stagger" :style="{ animationDelay: `${idx * 40}ms` }" aria-hidden="true" />
    </button>
  </div>
</template>

<style scoped>
.tabs {
  position: relative;
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  background: var(--surface-3);
  border-radius: var(--r-md);
  border: 1px solid var(--line);
}
.tabs.is-block {
  display: flex;
  width: 100%;
}
.tabs.is-block .tab-item {
  flex: 1;
  justify-content: center;
}

.indicator {
  position: absolute;
  top: 3px;
  left: 0;
  height: calc(100% - 6px);
  border-radius: calc(var(--r-md) - 3px);
  background: var(--surface);
  box-shadow: var(--sh-sm);
  transition: transform var(--dur-base) var(--ease-spring), width var(--dur-base) var(--ease-spring);
  z-index: 0;
}

.tab-item {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: calc(var(--r-md) - 3px);
  color: var(--text-3);
  font-weight: 600;
  transition: color var(--dur-base) var(--ease-out);
  white-space: nowrap;
}

.sz-sm .tab-item {
  height: 28px;
  padding: 0 12px;
  font-size: 12.5px;
}
.sz-md .tab-item {
  height: 34px;
  padding: 0 15px;
  font-size: 13px;
}

.tab-item.on {
  color: var(--brand-strong);
}
.tab-item:hover:not(.on) {
  color: var(--text-1);
}

.badge {
  min-width: 17px;
  height: 17px;
  padding: 0 5px;
  border-radius: var(--r-full);
  font-size: 10.5px;
  font-weight: 700;
  display: grid;
  place-items: center;
  background: rgba(var(--brand-rgb, 99), 102, 241, 0.12);
  background: rgba(99, 102, 241, 0.12);
  color: var(--brand);
}
.tab-item.on .badge {
  background: var(--grad-brand);
  color: #fff;
}

.stagger {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: rgba(99, 102, 241, 0.06);
  opacity: 0;
  animation: fade-in var(--dur-base) var(--ease-out) both;
}
</style>
