import { call } from './http'
import { db, delay, delayVoid, uid } from './mock/db'
import type { ChapterOutline, OutlineGenerationResult, ProposedEntity, Volume } from '@/types'

const AI_TITLES = [
  '破壳之前',
  '第一声啼鸣',
  '冷了的夜',
  '不是草鸡',
  '镇口的那辆车',
  '谁也不知道的东西',
  '一张旧契',
  '十四枚灵石',
  '雨里的脚印',
  '第一次实战',
  '被看轻的人',
  '一道裂缝',
  '那年冬猎',
  '主人的名字',
  '看不见的丝线',
  '交换的条件',
  '谁在跟着',
  '离开矿镇'
]

const ROLE_POOL = ['林昭', '灰羽', '苏晚', '裴临', '岩甲蟹']

function makeBeats(i: number, eventHint?: string): string[] {
  const n = 3 + (i % 5)
  const base = eventHint
    ? [`承接上一章余波：${eventHint.slice(0, 16)}…`, '出现一个意料之外的阻碍', '用非正统方式化解', '代价与收获同时出现', '章末钩子']
    : ['开场场景与情绪铺垫', '本段核心冲突出现', '一次尝试失败', '换思路再干一次', '小胜但有隐患', '章末钩子']
  return base.slice(0, Math.max(3, Math.min(7, n)))
}

export const outlineApi = {
  /** GET /api/novels/{id}/volumes */
  volumes(novelId: number) {
    return call<Volume[]>('GET', `/novels/${novelId}/volumes`, undefined, async () => {
      const list = db.volumes.filter((v) => v.novelId === novelId).sort((a, b) => a.volumeNo - b.volumeNo)
      return delay(list, 380)
    })
  },

  /** POST /api/novels/{id}/volumes */
  createVolume(novelId: number, title: string) {
    return call<Volume>('POST', `/novels/${novelId}/volumes`, { title }, async () => {
      const list = db.volumes.filter((v) => v.novelId === novelId)
      const v: Volume = {
        id: Number(`${novelId}${String(list.length + 1)}`),
        novelId,
        volumeNo: list.length + 1,
        title,
        events: [],
        status: 'RAW',
        targetChapters: 18,
        createdAt: new Date().toISOString()
      }
      db.volumes.push(v)
      return delay(v, 480)
    })
  },

  /** PUT /api/volumes/{id}/events */
  saveEvents(volumeId: number, events: string[]) {
    return call<Volume>('PUT', `/volumes/${volumeId}/events`, { events }, async () => {
      const v = db.volumes.find((x) => x.id === volumeId)
      if (!v) throw new Error('卷不存在')
      v.events = events.map((content, i) => ({ id: i + 1, sort: i + 1, content }))
      if (v.status === 'RAW') v.status = 'OUTLINED'
      return delay(v, 420)
    })
  },

  /** GET /api/volumes/{id}/outlines */
  outlines(volumeId: number) {
    return call<ChapterOutline[]>('GET', `/volumes/${volumeId}/outlines`, undefined, async () => {
      const list = db.outlines.filter((o) => o.volumeId === volumeId).sort((a, b) => a.chapterNo - b.chapterNo)
      return delay(list, 340)
    })
  },

  /** POST /api/volumes/{id}/outline-generation —— AI 拆本卷章纲 */
  generate(volumeId: number) {
    return call<OutlineGenerationResult>('POST', `/volumes/${volumeId}/outline-generation`, undefined, async () => {
      await delayVoid(1600)
      const v = db.volumes.find((x) => x.id === volumeId)
      if (!v) throw new Error('卷不存在')

      // 已确认的章纲保留，只补充/覆盖草稿部分
      const exist = db.outlines.filter((o) => o.volumeId === volumeId)
      const total = v.targetChapters ?? 18
      const confirmed = exist.filter((o) => o.status === 'CONFIRMED')
      const fresh: ChapterOutline[] = []
      for (let i = confirmed.length + 1; i <= total; i++) {
        fresh.push({
          id: Number(`${volumeId}${String(i).padStart(3, '0')}`),
          volumeId,
          chapterNo: i,
          title: `${i}. ${AI_TITLES[(i - 1) % AI_TITLES.length]}`,
          beats: makeBeats(i, v.events[(i - 1) % Math.max(1, v.events.length)]?.content),
          characters: ROLE_POOL.filter((_, k) => k === 0 || (i + k) % 3 === 0).slice(0, 3),
          estimatedWords: 2300 + ((i * 37) % 400),
          status: 'DRAFT',
          version: 1
        })
      }
      db.outlines = [...db.outlines.filter((o) => o.volumeId !== volumeId), ...confirmed, ...fresh]
      v.status = 'PENDING_CONFIRM'

      const proposed: ProposedEntity[] = [
        { id: uid('pe'), type: 'CHARACTER', name: '岩甲蟹', reason: '章 9 的实战对象，设定库中尚未登记' },
        { id: uid('pe'), type: 'ITEM', name: '青崖特招令', reason: '章 18 作为道具首次出现' }
      ]
      return { volumeId, outlines: [...confirmed, ...fresh], proposedEntities: proposed }
    })
  },

  /** PUT /api/volumes/{id}/outlines/confirm —— 批量确认 */
  confirm(volumeId: number) {
    return call<Volume>('PUT', `/volumes/${volumeId}/outlines/confirm`, undefined, async () => {
      await delayVoid(700)
      const v = db.volumes.find((x) => x.id === volumeId)
      if (!v) throw new Error('卷不存在')
      db.outlines.filter((o) => o.volumeId === volumeId).forEach((o) => { o.status = 'CONFIRMED' })
      v.status = 'CONFIRMED'

      // 确认后章节进入章节列表，状态 OUTLINED
      const novelId = v.novelId
      const outlines = db.outlines.filter((o) => o.volumeId === volumeId)
      outlines.forEach((o) => {
        const exist = db.chapters.find((c) => c.outlineId === o.id)
        if (exist) {
          exist.title = o.title
          exist.chapterNo = o.chapterNo
        } else {
          db.chapters.push({
            id: Number(`${novelId}${String(o.chapterNo).padStart(4, '0')}`),
            novelId,
            volumeId,
            outlineId: o.id,
            chapterNo: o.chapterNo,
            title: o.title,
            contentMd: '',
            wordCount: 0,
            chapterStatus: 'OUTLINED',
            auditStatus: 'NOT_RUN',
            contentVersion: 1,
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
          })
        }
      })
      return v
    })
  },

  /** PUT /api/outlines/{id} */
  update(id: number, patch: Partial<ChapterOutline>) {
    return call<ChapterOutline>('PUT', `/outlines/${id}`, patch, async () => {
      const o = db.outlines.find((x) => x.id === id)
      if (!o) throw new Error('章纲不存在')
      Object.assign(o, patch, { version: o.version + 1 })
      return delay(o, 300)
    })
  },

  remove(id: number) {
    return call<null>('DELETE', `/outlines/${id}`, undefined, async () => {
      db.outlines = db.outlines.filter((o) => o.id !== id)
      return delayVoid(300)
    })
  },

  insert(volumeId: number, afterChapterNo: number) {
    return call<ChapterOutline>('POST', `/volumes/${volumeId}/outlines`, { afterChapterNo }, async () => {
      const list = db.outlines.filter((o) => o.volumeId === volumeId)
      const o: ChapterOutline = {
        id: Number(uid('').replace('id_', '') || Date.now().toString().slice(-6)),
        volumeId,
        chapterNo: afterChapterNo + 1,
        title: `${afterChapterNo + 1}. 新章`,
        beats: ['待补充 beat 1', '待补充 beat 2', '待补充 beat 3'],
        characters: ['林昭'],
        estimatedWords: 2500,
        status: 'DRAFT',
        version: 1
      }
      list.filter((x) => x.chapterNo > afterChapterNo).forEach((x) => (x.chapterNo += 1))
      db.outlines.push(o)
      return delay(o, 340)
    })
  },

  /** PUT /api/volumes/{id}/outlines/reorder */
  reorder(volumeId: number, orders: { id: number; chapterNo: number }[]) {
    return call<null>('PUT', `/volumes/${volumeId}/outlines/reorder`, { orders }, async () => {
      orders.forEach(({ id, chapterNo }) => {
        const o = db.outlines.find((x) => x.id === id)
        if (o) {
          o.chapterNo = chapterNo
          o.title = o.title.replace(/^\d+\./, `${chapterNo}.`)
        }
      })
      return delayVoid(320)
    })
  }
}
