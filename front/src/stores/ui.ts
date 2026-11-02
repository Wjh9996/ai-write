import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ToastType = 'success' | 'error' | 'info' | 'warn'
export interface ToastItem {
  id: number
  type: ToastType
  title: string
  desc?: string
}

let seed = 0

export const useUiStore = defineStore('ui', () => {
  const toasts = ref<ToastItem[]>([])
  const globalLoading = ref(false)
  const loadingText = ref('')

  function toast(type: ToastType, title: string, desc?: string) {
    const id = ++seed
    toasts.value.push({ id, type, title, desc })
    setTimeout(() => {
      toasts.value = toasts.value.filter((t) => t.id !== id)
    }, 3200)
  }

  function success(title: string, desc?: string) {
    toast('success', title, desc)
  }
  function error(title: string, desc?: string) {
    toast('error', title, desc)
  }
  function info(title: string, desc?: string) {
    toast('info', title, desc)
  }
  function warn(title: string, desc?: string) {
    toast('warn', title, desc)
  }

  function showLoading(text = '加载中…') {
    globalLoading.value = true
    loadingText.value = text
  }
  function hideLoading() {
    globalLoading.value = false
    loadingText.value = ''
  }

  return { toasts, globalLoading, loadingText, toast, success, error, info, warn, showLoading, hideLoading }
})
