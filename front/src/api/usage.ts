import { call } from './http'
import { db, delay } from './mock/db'
import type { UsageOverview } from '@/types'

export const usageApi = {
  /** GET /api/usage */
  overview() {
    return call<UsageOverview>('GET', '/usage', undefined, async () => {
      const records = db.usage
      const monthCost = records.reduce((a, r) => a + r.estimatedCost, 0)
      const monthTokens = records.reduce((a, r) => a + r.inputTokens + r.outputTokens, 0)
      const map = new Map<string, { scene: string; cost: number; times: number }>()
      records.forEach((r) => {
        const hit = map.get(r.scene) ?? { scene: r.scene, cost: 0, times: 0 }
        hit.cost += r.estimatedCost
        hit.times += 1
        map.set(r.scene, hit)
      })
      return delay(
        {
          monthCost: Number(monthCost.toFixed(2)),
          monthTokens,
          chapterThisMonth: db.chapters.filter((c) => c.wordCount > 0).length,
          budget: 100,
          byScene: [...map.values()].sort((a, b) => b.cost - a.cost),
          records
        },
        360
      )
    })
  }
}
