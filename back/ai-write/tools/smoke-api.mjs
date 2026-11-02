/**
 * 后端接口冒烟：注册 → 建作品 → 卷事件 → AI 拆章纲 → 确认 → 生成（SSE）→ 自检 → 入库
 * 用法：node tools/smoke-api.mjs [baseUrl]
 */
const BASE = process.argv[2] || 'http://127.0.0.1:8088'

let token = ''
let failed = 0

function check(name, ok, extra = '') {
  if (ok) console.log(`  PASS  ${name}${extra ? ' — ' + extra : ''}`)
  else {
    failed++
    console.log(`  FAIL  ${name}${extra ? ' — ' + extra : ''}`)
  }
}

async function req(path, { method = 'GET', body, raw = false } = {}) {
  const res = await fetch(BASE + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  if (raw) return res
  const json = await res.json()
  if (json.code !== 0) throw new Error(`${method} ${path} -> ${json.code} ${json.message}`)
  return json.data
}

const rnd = Math.random().toString(36).slice(2, 7)
const email = `smoke_${rnd}@ai-write.dev`

console.log('\n[1] 账号')
const profile = await req('/api/auth/register', {
  method: 'POST',
  body: { email, password: 'test1234' }
})
token = profile.token
check('注册并拿到 token', !!token, token.slice(0, 8) + '…')
const me = await req('/api/auth/me')
check('GET /api/auth/me', me.email === email, me.email)

console.log('\n[2] 作品')
const novel = await req('/api/novels', {
  method: 'POST',
  body: { title: '冒烟测试作品', intro: '自动化验证用', mainOutline: '开局捡到一枚蛋 → 孵化 → 出山 → 成名' }
})
check('创建作品', !!novel.id, `#${novel.id} ${novel.title}`)
const list = await req('/api/novels')
check('作品列表', list.length >= 1, `${list.length} 部`)
await req(`/api/novels/${novel.id}/outline`, {
  method: 'PUT',
  body: { mainOutline: '主线：捡蛋 → 孵化 → 出山 → 成名' }
})
check('保存总纲', true)

console.log('\n[3] 卷与章纲')
let volumes = await req(`/api/novels/${novel.id}/volumes`)
check('建作品时自动建第一卷', volumes.length === 1, `${volumes.length} 卷`)
const vol = volumes[0]
await req(`/api/volumes/${vol.id}/events`, {
  method: 'PUT',
  body: { events: ['被发配守废矿', '发现一枚灰蛋', '孵化失败三次', '用体温孵出来', '走出矿镇'] }
})
check('保存阶段事件', true, '5 条')

const generated = await req(`/api/volumes/${vol.id}/outline-generation`, { method: 'POST' })
check('AI 拆章纲', generated.outlines.length > 0, `${generated.outlines.length} 条`)
const beatsOk = generated.outlines.every((o) => o.beats.length >= 3 && o.beats.length <= 7)
check('beat 数在 3—7 区间', beatsOk)
check('新实体走「待新增设定」', generated.proposedEntities.length > 0,
  generated.proposedEntities.map((e) => e.name).join('、'))

await req(`/api/volumes/${vol.id}/outlines/confirm`, { method: 'PUT' })
volumes = await req(`/api/novels/${novel.id}/volumes`)
check('确认后卷状态 CONFIRMED', volumes[0].status === 'CONFIRMED')

console.log('\n[4] 章节')
const chapters = await req(`/api/novels/${novel.id}/chapters`)
check('确认章纲后生成章节', chapters.length > 0, `${chapters.length} 章`)
check('章节初始状态 OUTLINED', chapters[0].chapterStatus === 'OUTLINED')
const first = chapters[0]

console.log('\n[5] 生成（SSE）')
const job = await req(`/api/chapters/${first.id}/generation-jobs`, {
  method: 'POST',
  body: { idempotencyKey: `smoke-${rnd}` }
})
check('创建生成任务', !!job.jobId, `jobId=${job.jobId.slice(0, 8)}… 共 ${job.totalSegments} 段`)

const t0 = Date.now()
const events = await collectSse(job.jobId, 90000)
console.log(`  (SSE 收集耗时 ${((Date.now() - t0) / 1000).toFixed(1)}s)`)
const types = new Set(events.map((e) => e.type))
check('收到 job.started', types.has('job.started'))
check('收到 segment.started', [...types].some((t) => t === 'segment.started'))
check('收到 text.delta', types.has('text.delta'))
check('收到 segment.completed', types.has('segment.completed'))
check('收到 chapter.draft_ready', types.has('chapter.draft_ready'))
check('收到 job.completed', types.has('job.completed'))
const seqs = events.map((e) => e.seq)
check('事件 seq 单调递增', seqs.every((s, i) => i === 0 || s > seqs[i - 1]), `${seqs.length} 个事件`)

const after = await req(`/api/chapters/${first.id}`)
check('生成后章节状态 REVIEW_PENDING', after.chapterStatus === 'REVIEW_PENDING')
check('字数落在 2300—2700', after.wordCount >= 2300 && after.wordCount <= 2700, `${after.wordCount} 字`)

console.log('\n[6] 幂等与并发规则')
const retry = await req(`/api/chapters/${first.id}/generation-jobs`, {
  method: 'POST',
  body: { idempotencyKey: `smoke-${rnd}` }
})
check('同幂等键返回同一任务（不产生重复正文）', retry.jobId === job.jobId, retry.jobId.slice(0, 8) + '…')

console.log('\n[7] 乐观锁')
const v0 = after.contentVersion
let conflict = false
try {
  await req(`/api/chapters/${first.id}/content`, {
    method: 'PUT',
    body: { contentMd: after.contentMd + '\n\n补一句。', expectedVersion: v0 - 1 }
  })
} catch (e) {
  conflict = e.message.includes('409')
}
check('旧版本号触发 409', conflict, `当前 v${v0}`)
const saved = await req(`/api/chapters/${first.id}/content`, {
  method: 'PUT',
  body: { contentMd: after.contentMd + '\n\n补一句。', expectedVersion: v0 }
})
check('正确版本号保存成功', saved.contentVersion === v0 + 1, `v${v0} → v${saved.contentVersion}`)

console.log('\n[8] 审核入库')
const draft = await req(`/api/chapters/${first.id}/review-draft`, { method: 'POST' })
check('生成自检草稿', Array.isArray(draft.issues), `${draft.issues.length} 个问题`)
check('生成摘要草稿', !!draft.summary.content, `${draft.summary.content.length} 字`)
check('生成变更草稿', draft.changes.length > 0, `${draft.changes.length} 条`)
await req(`/api/chapters/${first.id}/approve`, {
  method: 'POST',
  body: { summary: draft.summary.content, changes: draft.changes }
})
const approved = await req(`/api/chapters/${first.id}`)
check('入库后状态 APPROVED', approved.chapterStatus === 'APPROVED')

console.log('\n[9] 设定库 / 用量 / 导出')
const settings = await req(`/api/novels/${novel.id}/settings`)
check('设定库可读', !!settings, `实体 ${settings.entities?.length ?? 0} 条`)
const usage = await req('/api/usage')
check('用量统计', usage.monthCost >= 0, `¥${usage.monthCost} · ${usage.byScene.length} 个场景`)
const meta = await req(`/api/novels/${novel.id}/export/meta?format=docx&scope=BOOK`)
check('导出 docx', meta.fileName.endsWith('.docx'), `${meta.fileName} · ${meta.sizeKb}KB`)

console.log(`\n===== ${failed === 0 ? '全部通过' : failed + ' 项失败'} =====`)
process.exit(failed ? 1 : 0)

/** 收集 SSE 事件直到收到 job.completed 或超时 */
async function collectSse(jobId, timeoutMs) {
  const events = []
  const url = `${BASE}/api/generation-jobs/${jobId}/events?token=${encodeURIComponent(token)}`
  const ac = new AbortController()
  const timer = setTimeout(() => ac.abort(), timeoutMs)

  const res = await fetch(url, { headers: { Accept: 'text/event-stream' }, signal: ac.signal })
  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  let buf = ''
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      let idx
      while ((idx = buf.indexOf('\n\n')) >= 0) {
        const frame = buf.slice(0, idx)
        buf = buf.slice(idx + 2)
        const dataLine = frame.split('\n').find((l) => l.startsWith('data:'))
        if (!dataLine) continue
        const payload = dataLine.slice(5).trim()
        if (payload === '[DONE]') break
        try {
          const evt = JSON.parse(payload)
          events.push(evt)
          if (evt.type === 'job.completed' || evt.type === 'job.stopped' || evt.type === 'job.failed') {
            clearTimeout(timer)
            ac.abort()
            return events
          }
        } catch {
          /* 忽略非 JSON 帧 */
        }
      }
    }
  } catch {
    /* abort 或连接关闭 */
  } finally {
    clearTimeout(timer)
  }
  return events
}
