<script setup lang="ts">
/** 输入框：浮动标签 + 焦点发光 */
import { computed, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: string
    label?: string
    placeholder?: string
    type?: string
    disabled?: boolean
    icon?: string
    maxlength?: number
    error?: string
  }>(),
  { type: 'text' }
)
const emit = defineEmits<{ 'update:modelValue': [string] }>()

const focused = ref(false)
const hasValue = computed(() => props.modelValue.length > 0)
</script>

<template>
  <div class="field" :class="{ 'is-focus': focused, 'is-err': !!error }">
    <label v-if="label" class="lb">{{ label }}</label>
    <div class="box">
      <slot name="prefix" />
      <input
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :maxlength="maxlength"
        @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        @focus="focused = true"
        @blur="focused = false"
      />
      <span v-if="maxlength && hasValue" class="counter mono">{{ modelValue.length }}/{{ maxlength }}</span>
    </div>
    <p v-if="error" class="err">{{ error }}</p>
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.lb {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
}

.box {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 12px;
  border-radius: var(--r-md);
  border: 1.5px solid var(--line-2);
  background: var(--surface);
  transition: border-color var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out);
}

.field.is-focus .box {
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.14);
}
.field.is-err .box {
  border-color: var(--c-danger);
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}

input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13.5px;
  color: var(--text-1);
}
input::placeholder {
  color: var(--text-4);
}
input:disabled {
  color: var(--text-3);
}

.counter {
  font-size: 11px;
  color: var(--text-4);
}

.err {
  font-size: 11.5px;
  color: var(--c-danger);
}
</style>
