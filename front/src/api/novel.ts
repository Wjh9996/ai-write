import { call } from './http'
import { db, delay, delayVoid, uid } from './mock/db'
import { buildData } from './mock/seed'
import type { CreateNovelPayload, Novel, NovelStats } from '@/types'

const GRADIENTS = [
  'linear-gradient(135deg,#6366f1 0%,#a855f7 45%,#ec4899 100%)',
  'linear-gradient(135deg,#38bdf8 0%,#6366f1 100%)',
  'linear-gradient(135deg,#34d399 0%,#22d3ee 100%)',
  'linear-gradient(135deg,#fb923c 0%,#f43f5e 100%)',
  'linear-gradient(135deg,#fbbf24 0%,#f97316 100%)'
]

function calcStats(novelId: number): NovelStats {
  const list = db.chapters.filter((c) => c.novelId === novelId)
  return {
    totalWords: list.reduce((a, c) => a + c.wordCount, 0),
    chapterCount: db.outlines.filter((o) => o.volumeId.toString().startsWith(String(novelId))).length,
    approvedCount: list.filter((c) => c.chapterStatus === 'APPROVED').length,
    reviewPendingCount: list.filter((c) => c.chapterStatus === 'REVIEW_PENDING').length,
    generatingCount: list.filter((c) => c.chapterStatus === 'GENERATING').length,
    outlinedCount: db.outlines.length,
    volumeCount: db.volumes.filter((v) => v.novelId === novelId).length
  }
}

export const novelApi = {
  /** GET /api/novels */
  list() {
    return call<Novel[]>('GET', '/novels', undefined, async () => {
      const list = db.novels.map((n) => ({ ...n, stats: calcStats(n.id) }))
      return delay(list, 420)
    })
  },

  /** GET /api/novels/{id} */
  detail(id: number) {
    return call<Novel>('GET', `/novels/${id}`, undefined, async () => {
      const n = db.novels.find((x) => x.id === id)
      if (!n) throw new Error('作品不存在')
      return delay({ ...n, stats: calcStats(id) }, 320)
    })
  },

  /** POST /api/novels */
  create(payload: CreateNovelPayload) {
    return call<Novel>('POST', '/novels', payload, async () => {
      const id = Math.max(...db.novels.map((n) => n.id)) + 1
      const novel: Novel = {
        id,
        userId: 1,
        title: payload.title,
        intro: payload.intro ?? '',
        mainOutline: payload.mainOutline ?? '',
        coverGradient: GRADIENTS[id % GRADIENTS.length],
        status: 'DRAFT',
        version: 1,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        stats: {
          totalWords: 0,
          chapterCount: 0,
          approvedCount: 0,
          reviewPendingCount: 0,
          generatingCount: 0,
          outlinedCount: 0,
          volumeCount: 0
        }
      }
      db.novels.unshift(novel)
      // 新作品自动建第一卷
      db.volumes.push({
        id: id * 100 + 1,
        novelId: id,
        volumeNo: 1,
        title: '第一卷 · 开端',
        events: [],
        status: 'RAW',
        targetChapters: 18,
        createdAt: new Date().toISOString()
      })
      db.style[id] = { ...buildData.style(id), id, novelId: id }
      return delay(novel, 560)
    })
  },

  /** PUT /api/novels/{id}/outline */
  updateOutline(id: number, mainOutline: string) {
    return call<Novel>('PUT', `/novels/${id}/outline`, { mainOutline }, async () => {
      const n = db.novels.find((x) => x.id === id)
      if (!n) throw new Error('作品不存在')
      n.mainOutline = mainOutline
      n.version += 1
      n.updatedAt = new Date().toISOString()
      return delay({ ...n, stats: calcStats(id) }, 420)
    })
  },

  /** POST /api/novels/{id}/outline/import —— TXT 导入 */
  async importTxt(id: number, file: File) {
    return call<Novel>('POST', `/novels/${id}/outline/import`, {}, async () => {
      const text = await file.text()
      const n = db.novels.find((x) => x.id === id)
      if (!n) throw new Error('作品不存在')
      n.mainOutline = `[导入自 ${file.name}]\n${text}`
      n.version += 1
      // 按行尝试拆出卷
      const lines = text
        .split(/\r?\n/)
        .map((l) => l.trim())
        .filter(Boolean)
      const volumeLines = lines.filter((l) => /^第[一二三四五六七八九十]+卷/.test(l))
      if (volumeLines.length) {
        db.volumes = db.volumes.filter((v) => v.novelId !== id)
        volumeLines.forEach((l, i) => {
          db.volumes.push({
            id: Number(`${id}${i + 1}`),
            novelId: id,
            volumeNo: i + 1,
            title: l.replace(/^【|】$/g, '').slice(0, 30),
            events: [],
            status: 'RAW',
            targetChapters: 18,
            createdAt: new Date().toISOString()
          })
        })
      }
      return delay({ ...n, stats: calcStats(id) }, 900)
    })
  },

  /** DELETE /api/novels/{id} */
  remove(id: number) {
    return call<null>('DELETE', `/novels/${id}`, undefined, async () => {
      db.novels = db.novels.filter((n) => n.id !== id)
      db.volumes = db.volumes.filter((v) => v.novelId !== id)
      return delayVoid(360)
    })
  }
}

export { uid, delay }
