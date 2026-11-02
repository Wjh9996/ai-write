import type { ApiResult } from '@/types'

/**
 * 统一请求层
 * VITE_USE_MOCK = true（默认）时走内置 mock 数据，后端就绪后设为 false 即可切到真实 REST
 */
export const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

const BASE = import.meta.env.VITE_API_BASE ?? '/api'
const TOKEN_KEY = 'ai_write_token'

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

/** 安全的本地存储访问：非浏览器环境（Node / SSR）自动降级为内存态 */
const memoryToken: Record<string, string> = {}

function ls(): Storage | null {
  try {
    return typeof localStorage !== 'undefined' ? localStorage : null
  } catch {
    return null
  }
}

export function getToken(): string {
  return ls()?.getItem(TOKEN_KEY) ?? memoryToken[TOKEN_KEY] ?? ''
}

export function setToken(token: string): void {
  memoryToken[TOKEN_KEY] = token
  ls()?.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  delete memoryToken[TOKEN_KEY]
  ls()?.removeItem(TOKEN_KEY)
}

/**
 * 统一调用入口：自动在 mock 与真实 HTTP 之间切换
 * @param method HTTP 方法
 * @param path   以 /api 之后的部分，例如 '/novels'
 * @param body   请求体
 * @param mockFn mock 分支的数据生产函数
 */
export async function call<T>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  path: string,
  body?: unknown,
  mockFn?: () => Promise<T> | T
): Promise<T> {
  if (USE_MOCK) {
    if (!mockFn) throw new ApiError(500, `未提供 mock 实现：${method} ${path}`)
    return await mockFn()
  }

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(getToken() ? { Authorization: `Bearer ${getToken()}` } : {})
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  })

  let json: ApiResult<T>
  try {
    json = (await res.json()) as ApiResult<T>
  } catch {
    throw new ApiError(res.status, '服务端返回了非 JSON 响应')
  }

  if (json.code !== 0 || !res.ok) {
    if (res.status === 401) clearToken()
    throw new ApiError(json.code || res.status, json.message || '请求失败')
  }
  return json.data
}

/**
 * SSE 事件流消费
 * 真实模式：EventSource（携带 token 走 query，因 EventSource 不支持自定义 header）
 * mock 模式：由调用方传入的模拟生成器驱动
 */
export interface StreamHandle {
  close: () => void
}

export interface OpenStreamOptions<T> {
  path: string
  lastEventId?: number
  /** mock 模式下由 mockFactory 的 emit 驱动，此项可不传 */
  onEvent?: (data: T) => void
  onError?: (err: unknown) => void
  mockFactory?: (emit: (data: T) => void, close: () => void) => () => void
}

export function openStream<T>(opts: OpenStreamOptions<T>): StreamHandle {
  const { path, lastEventId, onEvent, onError, mockFactory } = opts

  if (mockFactory) {
    let closed = false
    const cancel = mockFactory(
      (data: T) => {
        if (!closed) onEvent?.(data)
      },
      () => {
        closed = true
      }
    )
    return {
      close: () => {
        closed = true
        cancel()
      }
    }
  }

  const sep = path.includes('?') ? '&' : '?'
  const url =
    `${BASE}${path}${sep}token=${encodeURIComponent(getToken())}` +
    (lastEventId ? `&lastEventId=${lastEventId}` : '')

  const es = new EventSource(url)
  es.onmessage = (e) => {
    try {
      onEvent?.(JSON.parse(e.data) as T)
    } catch (err) {
      onError?.(err)
    }
  }
  es.onerror = (err) => {
    onError?.(err)
  }
  return { close: () => es.close() }
}
