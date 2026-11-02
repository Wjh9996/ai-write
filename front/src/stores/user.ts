import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import type { LoginPayload, UserProfile } from '@/types'

export const useUserStore = defineStore('user', () => {
  const profile = ref<UserProfile | null>(null)
  const loading = ref(false)
  const initialized = ref(false)

  const isLogin = computed(() => !!profile.value)
  const quotaPercent = computed(() => {
    if (!profile.value) return 0
    return Math.min(100, Math.round((profile.value.usedChars / profile.value.monthlyQuotaChars) * 100))
  })

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      profile.value = await authApi.login(payload)
      initialized.value = true
      return true
    } finally {
      loading.value = false
    }
  }

  async function load() {
    if (initialized.value) return
    loading.value = true
    try {
      profile.value = await authApi.me()
    } catch {
      profile.value = null
    } finally {
      initialized.value = true
      loading.value = false
    }
  }

  async function logout() {
    await authApi.logout()
    profile.value = null
  }

  return { profile, loading, initialized, isLogin, quotaPercent, login, load, logout }
})
