import { call } from './http'
import { db, delay, delayVoid } from './mock/db'
import { SEGMENT_TEXTS } from './mock/streamText'
import type { Chapter, ChapterVersion } from '@/types'

export const chapterApi = {
  /** GET /api/novels/{id}/chapters */
  list(novelId: number) {
    return call<Chapter[]>('GET', `/novels/${novelId}/chapters`, undefined, async () => {
      const list = db.chapters.filter((c) => c.novelId === novelId).sort((a, b) => a.chapterNo - b.chapterNo)
      return delay(list, 380)
    })
  },

  detail(id: number) {
    return call<Chapter>('GET', `/chapters/${id}`, undefined, async () => {
      const c = db.chapters.find((x) => x.id === id)
      if (!c) throw new Error('章节不存在')
      return delay({ ...c }, 300)
    })
  },

  /**
   * PUT /api/chapters/{id}/content
   * 携带 expectedVersion 做乐观锁
   */
  saveContent(id: number, contentMd: string, expectedVersion: number) {
    return call<Chapter>('PUT', `/chapters/${id}/content`, { contentMd, expectedVersion }, async () => {
      await delayVoid(320)
      const c = db.chapters.find((x) => x.id === id)
      if (!c) throw new Error('章节不存在')
      if (expectedVersion !== c.contentVersion) {
        const err = new Error('正文已被其他操作更新，请刷新后再保存')
        ;(err as Error & { code?: number }).code = 409
        throw err
      }
      c.contentMd = contentMd
      c.wordCount = countWords(contentMd)
      c.contentVersion += 1
      c.updatedAt = new Date().toISOString()
      return { ...c }
    })
  },

  versions(id: number) {
    return call<ChapterVersion[]>('GET', `/chapters/${id}/versions`, undefined, async () => {
      const c = db.chapters.find((x) => x.id === id)
      if (!c) return []
      const list: ChapterVersion[] = [
        {
          id: c.contentVersion * 10,
          chapterId: id,
          versionNo: c.contentVersion,
          contentMd: c.contentMd,
          editorType: 'HUMAN',
          createdAt: c.updatedAt
        },
        {
          id: c.contentVersion * 10 - 1,
          chapterId: id,
          versionNo: c.contentVersion - 1,
          contentMd: c.contentMd.slice(0, Math.max(0, c.contentMd.length - 200)) || '（空）',
          editorType: 'AI',
          createdAt: new Date(Date.now() - 3600_000).toISOString()
        }
      ]
      return delay(list.filter((v) => v.versionNo > 0), 300)
    })
  }
}

/** 中文字数统计：中文字符 + 英文单词 + 数字串 */
export function countWords(text: string): number {
  if (!text) return 0
  const cn = text.match(/[\u4e00-\u9fa5]/g)?.length ?? 0
  const en = text.match(/[a-zA-Z]+/g)?.length ?? 0
  const num = text.match(/\d+/g)?.length ?? 0
  return cn + en + num
}

export const SEGMENT_COUNT = SEGMENT_TEXTS.length
