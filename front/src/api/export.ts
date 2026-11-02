import { call, getToken, USE_MOCK } from './http'
import { db, delay, delayVoid } from './mock/db'
import type { ExportOption } from '@/types'

export const exportApi = {
  /**
   * 拿导出的元信息（JSON）。
   * 注意：真正的文件下载走 downloadUrl —— 后端 /export 返回的是二进制流，
   * 不能用 fetch 当 JSON 解析（会报「服务端返回了非 JSON 响应」）。
   */
  run(novelId: number, option: ExportOption) {
    return call<{ fileName: string; sizeKb: number; scope: string; format: string }>(
      'GET',
      `/novels/${novelId}/export/meta?format=${option.format}&scope=${option.scope}` +
        `&withTitle=${option.withTitle}&withAuthor=${option.withAuthor}`,
      undefined,
      async () => {
        await delayVoid(1800)
        const novel = db.novels.find((n) => n.id === novelId)
        const scopeMap = { CHAPTER: '当前章', VOLUME: '当前卷', BOOK: '全书' }
        const fileName = `${novel?.title ?? '未命名'}-${scopeMap[option.scope]}.${
          option.format === 'docx' ? 'docx' : option.format
        }`
        return {
          fileName,
          sizeKb: 128 + Math.round(Math.random() * 200),
          scope: option.scope,
          format: option.format
        }
      }
    )
  },

  /**
   * 浏览器直连下载链接。
   * 后端安全过滤器支持 ?token= 方式鉴权（EventSource / <a download> 无法自定义请求头），
   * 所以这里把 token 拼在 query 上。
   */
  downloadUrl(novelId: number, option: ExportOption): string {
    const base = USE_MOCK ? '/api' : (import.meta.env.VITE_API_BASE ?? '/api')
    const q = new URLSearchParams({
      format: option.format,
      scope: option.scope,
      withTitle: String(option.withTitle),
      withAuthor: String(option.withAuthor),
      token: getToken()
    })
    return `${base}/novels/${novelId}/export?${q.toString()}`
  },

  history(novelId: number) {
    return call<{ id: number; fileName: string; scope: string; createdAt: string; sizeKb: number }[]>(
      'GET',
      `/novels/${novelId}/export/history`,
      undefined,
      async () =>
        delay(
          [
            {
              id: 1,
              fileName: '御兽：从一枚灰蛋开始-第一卷.docx',
              scope: 'VOLUME',
              createdAt: '2026-10-28T21:04:00',
              sizeKb: 246
            },
            {
              id: 2,
              fileName: '御兽：从一枚灰蛋开始-第1-9章.docx',
              scope: 'CHAPTER',
              createdAt: '2026-10-26T11:22:00',
              sizeKb: 132
            }
          ],
          320
        )
    )
  }
}
