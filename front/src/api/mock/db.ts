import { buildData, buildNovel } from './seed'
import type {
  Chapter,
  ChapterOutline,
  Novel,
  SettingEntity,
  StyleProfile,
  UsageRecord,
  Volume,
  WorldSetting
} from '@/types'

/**
 * 内存数据库 —— Mock 模式下的数据结构，接口形态与后端一致
 */
interface Db {
  novels: Novel[]
  volumes: Volume[]
  outlines: ChapterOutline[]
  chapters: Chapter[]
  entities: SettingEntity[]
  worlds: WorldSetting[]
  usage: UsageRecord[]
  style: Record<number, StyleProfile>
}

const G1 = 'linear-gradient(135deg,#6366f1 0%,#a855f7 45%,#ec4899 100%)'
const G2 = 'linear-gradient(135deg,#38bdf8 0%,#6366f1 100%)'
const G3 = 'linear-gradient(135deg,#34d399 0%,#22d3ee 100%)'

function createDb(): Db {
  const n1 = buildNovel(1, '御兽：从一枚灰蛋开始', '讲述一个被发配守废矿的少年，从一枚灰蛋开始成为御兽大师的故事。', G1)
  const n2 = buildNovel(2, '旧城剑客事务所', '现代都市背景下的剑客工会，接委托、查真相、守规矩。', G2)
  const n3 = buildNovel(3, '虫巢纪元', '人类最后一座城，被虫群围了七十年。', G3)

  const db: Db = {
    novels: [n1, n2, n3],
    volumes: [...buildData.volumes(1), ...buildData.volumes(2), ...buildData.volumes(3)],
    outlines: [...buildData.outlines(1), ...buildData.outlines(2), ...buildData.outlines(3)],
    chapters: buildData.chapters(1),
    entities: buildData.entities(1),
    worlds: buildData.worlds(1),
    usage: buildData.usage(),
    style: { 1: buildData.style(1) }
  }

  db.novels.forEach((n) => {
    const list = db.chapters.filter((c) => c.novelId === n.id)
    const totalWords = list.reduce((a, c) => a + c.wordCount, 0)
    n.stats = {
      totalWords: totalWords || (n.id === 1 ? 24000 : 0),
      chapterCount: list.length || 18,
      approvedCount: list.filter((c) => c.chapterStatus === 'APPROVED').length,
      reviewPendingCount: list.filter((c) => c.chapterStatus === 'REVIEW_PENDING').length,
      generatingCount: list.filter((c) => c.chapterStatus === 'GENERATING').length,
      outlinedCount: 18,
      volumeCount: db.volumes.filter((v) => v.novelId === n.id).length
    }
  })
  // 非 1 号作品给一些视觉上合理的统计
  db.novels[1].stats = {
    totalWords: 12400,
    chapterCount: 6,
    approvedCount: 4,
    reviewPendingCount: 1,
    generatingCount: 0,
    outlinedCount: 12,
    volumeCount: 3
  }
  db.novels[2].stats = {
    totalWords: 3100,
    chapterCount: 2,
    approvedCount: 1,
    reviewPendingCount: 0,
    generatingCount: 0,
    outlinedCount: 8,
    volumeCount: 2
  }
  return db
}

export const db = createDb()

/** 延迟器：让 loading 骨架 / 动画有表现空间 */
export function delay<T>(data: T, ms = 380): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(structuredClone(data)), ms))
}

export function delayVoid(ms = 300): Promise<null> {
  return new Promise((resolve) => setTimeout(() => resolve(null), ms))
}

export function uid(prefix = 'id'): string {
  return `${prefix}_${Math.random().toString(36).slice(2, 9)}`
}

export function nowStr(): string {
  return new Date().toISOString()
}
