<script setup lang="ts">
/** 多行文本域：自动撑高（可选）、字数统计、焦点发光 */
import { computed, ref, watch, nextTick } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    placeholder?: string
    rows?: number
    autosize?: boolean
    maxHeight?: number
    disabled?: boolean
    showCount?: boolean
    mono?: boolean
  }>(),
  { rows: 6, maxHeight: 460 }
)
const emit = defineEmits<{ 'update:modelValue': [string] }>()

const el = ref<HTMLTextAreaElement | null>(null)
const focused = ref(false)
const count = computed(() => props.modelValue.length)

function resize() {
  if (!props.autosize || !el.value) return
  el.value.style.height = 'auto'
  el.value.style.height = `${Math.min(el.value.scrollHeight, props.maxHeight)}px`
}

watch(() => props.modelValue, () => nextTick(resize), { immediate: true })
</script>

<template>
  <div class="ta-wrap" :class="{ 'is-focus': focused }">
    <textarea
      ref="el"
      :value="modelValue"
      :rows="rows"
      :placeholder="placeholder"
      :disabled="disabled"
      :class="{ mono }"
      @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      @focus="focused = true"
      @blur="focused = false"
    />
    <span v-if="showCount" class="count mono">{{ count }}</span>
  </div>
</template>

<style scoped>
.ta-wrap {
  position: relative;
  border-radius: var(--r-md);
  border: 1.5px solid var(--line-2);
  background: var(--surface);
  transition: border-color var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out);
}
.ta-wrap.is-focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.14);
}

textarea {
  display: block;
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  padding: 11px 13px;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--text-1);
  resize: vertical;
  border-radius: inherit;
  font-family: inherit;
}
textarea.mono {
  font-family: var(--font-mono);
  font-size: 13px;
}
textarea::placeholder {
  color: var(--text-4);
}
textarea:disabled {
  color: var(--text-3);
  cursor: not-allowed;
}

.count {
  position: absolute;
  right: 10px;
  bottom: 8px;
  font-size: 11px;
  color: var(--text-4);
  background: rgba(255, 255, 255, 0.86);
  padding: 1px 6px;
  border-radius: 6px;
}
</style>
