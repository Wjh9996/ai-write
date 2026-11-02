<script setup lang="ts">
/** 开关 */
const props = withDefaults(defineProps<{ modelValue: boolean; disabled?: boolean }>(), {})
const emit = defineEmits<{ 'update:modelValue': [boolean] }>()
function toggle() {
  if (props.disabled) return
  emit('update:modelValue', !props.modelValue)
}
</script>

<template>
  <button
    type="button"
    class="sw"
    :class="{ on: modelValue, disabled }"
    :disabled="disabled"
    role="switch"
    :aria-checked="modelValue"
    @click="toggle"
  >
    <span class="knob" />
  </button>
</template>

<style scoped>
.sw {
  width: 40px;
  height: 22px;
  border-radius: var(--r-full);
  background: var(--line-2);
  position: relative;
  transition: background var(--dur-base) var(--ease-out);
  flex: none;
}
.sw.on {
  background: var(--grad-brand);
  box-shadow: 0 3px 10px -3px rgba(99, 102, 241, 0.6);
}
.sw.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.knob {
  position: absolute;
  top: 2.5px;
  left: 2.5px;
  width: 17px;
  height: 17px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.18);
  transition: transform var(--dur-base) var(--ease-spring);
}
.sw.on .knob {
  transform: translateX(18px);
}
</style>
