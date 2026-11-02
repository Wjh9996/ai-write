import { call } from './http'
import { db, delay, delayVoid } from './mock/db'
import { buildData } from './mock/seed'
import type {
  EntityType,
  Foreshadowing,
  NovelSettings,
  SettingEntity,
  StyleProfile,
  TimelineEvent,
  WorldSetting
} from '@/types'

export const settingsApi = {
  /** GET /api/novels/{id}/settings */
  get(novelId: number) {
    return call<NovelSettings>('GET', `/novels/${novelId}/settings`, undefined, async () => {
      if (!db.entities.some((e) => e.novelId === novelId)) {
        db.entities.push(...buildData.entities(novelId))
      }
      if (!db.worlds.some((w) => w.novelId === novelId)) {
        db.worlds.push(...buildData.worlds(novelId))
      }
      if (!db.style[novelId]) db.style[novelId] = { ...buildData.style(novelId), id: novelId, novelId }
      return delay(
        {
          entities: db.entities.filter((e) => e.novelId === novelId),
          worlds: db.worlds.filter((w) => w.novelId === novelId),
          timeline: buildData.timeline(novelId),
          foreshadowings: buildData.foreshadowings(novelId),
          style: db.style[novelId]
        },
        360
      )
    })
  },

  /** PUT /api/novels/{id}/settings */
  save(novelId: number, payload: NovelSettings) {
    return call<NovelSettings>('PUT', `/novels/${novelId}/settings`, payload, async () => {
      db.entities = [...db.entities.filter((e) => e.novelId !== novelId), ...payload.entities]
      db.worlds = [...db.worlds.filter((w) => w.novelId !== novelId), ...payload.worlds]
      if (payload.style) db.style[novelId] = payload.style
      return delay(payload, 420)
    })
  },

  /** AI 从总纲抽取设定初稿 —— P1，本地模拟增量 */
  extract(novelId: number) {
    return call<{ entities: SettingEntity[]; worlds: WorldSetting[] }>(
      'POST',
      `/novels/${novelId}/settings/extract`,
      undefined,
      async () => {
        await delayVoid(1500)
        const extra: SettingEntity[] = [
          {
            id: Date.now(),
            novelId,
            type: 'CHARACTER',
            name: '老陈',
            aliases: ['看矿的老头'],
            card: { 身份: '黑石废矿的老看守', 性格: '啰嗦，嘴上不留情但会给糖' },
            source: 'AI',
            approvalStatus: 'DRAFT',
            revealChapter: 4,
            updatedAt: new Date().toISOString()
          },
          {
            id: Date.now() + 1,
            novelId,
            type: 'ORGANIZATION',
            name: '林家',
            aliases: ['旁系'],
            card: { 地位: '本地小族，主脉在郡城', 对主角态度: '放养 / 放弃' },
            source: 'AI',
            approvalStatus: 'DRAFT',
            revealChapter: 1,
            updatedAt: new Date().toISOString()
          }
        ]
        const extraWorld: WorldSetting[] = [
          {
            id: Date.now(),
            novelId,
            category: '世界规则',
            title: '契约的反噬',
            content: '御兽师强行指挥超出自身灵脉承受的兽宠，会造成双方共同损伤。',
            source: 'AI',
            approvalStatus: 'DRAFT'
          }
        ]
        db.entities.push(...extra)
        db.worlds.push(...extraWorld)
        return { entities: extra, worlds: extraWorld }
      }
    )
  },

  saveEntity(novelId: number, entity: SettingEntity) {
    return call<SettingEntity>('PUT', `/entities/${entity.id}`, entity, async () => {
      const idx = db.entities.findIndex((e) => e.id === entity.id)
      if (idx >= 0) db.entities[idx] = { ...entity, updatedAt: new Date().toISOString() }
      else db.entities.push({ ...entity, id: Date.now(), novelId })
      return delay(entity, 340)
    })
  },

  removeEntity(id: number) {
    return call<null>('DELETE', `/entities/${id}`, undefined, async () => {
      db.entities = db.entities.filter((e) => e.id !== id)
      return delayVoid(300)
    })
  },

  createEntity(novelId: number, type: EntityType, name: string) {
    return call<SettingEntity>('POST', `/novels/${novelId}/entities`, { type, name }, async () => {
      const e: SettingEntity = {
        id: Date.now(),
        novelId,
        type,
        name,
        aliases: [],
        card: {},
        source: 'MANUAL',
        approvalStatus: 'DRAFT',
        revealChapter: null,
        updatedAt: new Date().toISOString()
      }
      db.entities.push(e)
      return delay(e, 360)
    })
  },

  saveTimeline(novelId: number, list: TimelineEvent[]) {
    return call<TimelineEvent[]>('PUT', `/novels/${novelId}/timeline`, list, async () => delay(list, 340))
  },

  saveForeshadowing(novelId: number, list: Foreshadowing[]) {
    return call<Foreshadowing[]>('PUT', `/novels/${novelId}/foreshadowing`, list, async () =>
      delay(list, 340)
    )
  },

  saveStyle(novelId: number, style: StyleProfile) {
    return call<StyleProfile>('PUT', `/novels/${novelId}/style`, style, async () => {
      db.style[novelId] = style
      return delay(style, 400)
    })
  }
}
