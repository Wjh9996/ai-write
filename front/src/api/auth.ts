import { call, getToken, setToken, clearToken, USE_MOCK } from './http'
import { db, delayVoid } from './mock/db'
import type { LoginPayload, UserProfile } from '@/types'

const PROFILE: UserProfile = {
  id: 1,
  email: 'author@ai-write.dev',
  nickname: '墨白',
  avatarColor: 'linear-gradient(135deg,#6366f1 0%,#ec4899 100%)',
  plan: 'PRO',
  monthlyQuotaChars: 120000,
  usedChars: 46800
}

function readProfile(): UserProfile {
  if (!getToken()) {
    // 方便本地开发：默认放一个 token，避免每次刷新都跳登录
    setToken('mock-token')
  }
  return PROFILE
}

export const authApi = {
  login(payload: LoginPayload) {
    return call<UserProfile>('POST', '/auth/login', payload, async () => {
      await delayVoid(700)
      if (!payload.email.includes('@')) throw new Error('邮箱格式不正确')
      setToken(`mock-token-${Date.now()}`)
      return { ...PROFILE }
    }).then((profile) => {
      // 真实后端会在返回体里带上 token，必须存下来供后续请求带 Authorization
      if (!USE_MOCK && profile?.token) setToken(profile.token)
      return profile
    })
  },

  me() {
    return call<UserProfile>('GET', '/auth/me', undefined, async () => {
      await delayVoid(200)
      return readProfile()
    })
  },

  logout() {
    return call<null>('POST', '/auth/logout', undefined, async () => {
      clearToken()
      return delayVoid(160)
    })
  }
}

export { db }
