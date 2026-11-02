import { call } from './http'
import { db, delayVoid } from './mock/db'
import { seed } from './mock/seed'
import { countWords } from './chapter'
import type { ReviewDraft, SettingChangeDraft } from '@/types'

const SUMMARY = `本章写林昭在感觉到掌心下蛋的动静后，把它带上地面，判断其为活体。夜里用工棚的炭盆三次尝试保温孵化，前两次失败，第三次用自身的体温配着最后半袋灵米粉熬过一夜，蛋壳终于在第四天清晨裂开。幼兽破壳后主动蹭他的掌心，契约当场成立，林昭为其取名"灰羽"。文末交代了他右掌洗不掉的矿灰，为后续血脉伏笔留下入口，并通过身后"被人注视"的不安感衔接下一章的事件。`

export const reviewApi = {
  /**
   * POST /api/chapters/{id}/review-draft
   * 按最新正文自检、生成摘要与设定变化草稿
   */
  draft(chapterId: number) {
    return call<ReviewDraft>('POST', `/chapters/${chapterId}/review-draft`, undefined, async () => {
      await delayVoid(1400)
      const c = db.chapters.find((x) => x.id === chapterId)
      if (!c) throw new Error('章节不存在')
      const issues = seed.issues().map((i) => ({ ...i }))
      const changes = seed.changes().map((x) => ({ ...x }))
      const draft: ReviewDraft = {
        chapterId,
        issues,
        summary: {
          content: SUMMARY,
          sourceContentVersion: c.contentVersion
        },
        changes,
        wordCount: countWords(c.contentMd),
        createdAt: new Date().toISOString()
      }
      c.auditStatus = issues.length ? 'NEEDS_REVIEW' : 'PASSED'
      return draft
    })
  },

  /**
   * POST /api/chapters/{id}/approve
   * 人工确认，事务性入库：摘要 + 角色状态 + 时间线 + 伏笔
   */
  approve(chapterId: number, payload: { summary: string; changes: SettingChangeDraft[]; notes?: string }) {
    return call<null>('POST', `/chapters/${chapterId}/approve`, payload, async () => {
      await delayVoid(900)
      const c = db.chapters.find((x) => x.id === chapterId)
      if (!c) throw new Error('章节不存在')
      const accepted = payload.changes.filter((x) => x.accepted)

      // 角色状态 / 新实体 / 时间线 / 伏笔在同一"事务"中落到设定库
      accepted.forEach((ch) => {
        if (ch.kind === 'NEW_ENTITY' && ch.name) {
          const exists = db.entities.find((e) => e.novelId === c.novelId && e.name === ch.name)
          if (!exists) {
            db.entities.push({
              id: Date.now() + Math.random(),
              novelId: c.novelId,
              type: ch.entityType ?? 'CHARACTER',
              name: ch.name,
              aliases: [],
              card: { 设定: ch.change },
              source: 'AI',
              approvalStatus: 'CONFIRMED',
              revealChapter: c.chapterNo,
              updatedAt: new Date().toISOString()
            })
          }
        }
        if (ch.kind === 'CHARACTER_UPDATE') {
          const e = db.entities.find((x) => x.novelId === c.novelId && x.name === ch.name)
          if (e) {
            e.card = { ...e.card, 最新变化: ch.change }
            e.approvalStatus = 'CONFIRMED'
            e.updatedAt = new Date().toISOString()
          }
        }
      })

      c.chapterStatus = 'APPROVED'
      c.auditStatus = 'PASSED'
      c.contentVersion += 1
      c.updatedAt = new Date().toISOString()
      return null
    })
  },

  /** 退回修改 */
  reject(chapterId: number, reason: string) {
    return call<null>('POST', `/chapters/${chapterId}/reject`, { reason }, async () => {
      await delayVoid(520)
      const c = db.chapters.find((x) => x.id === chapterId)
      if (!c) throw new Error('章节不存在')
      c.chapterStatus = 'GENERATING'
      c.auditStatus = 'NEEDS_REVIEW'
      return null
    })
  }
}
