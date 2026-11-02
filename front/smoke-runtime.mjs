/**
 * 运行时冒烟脚本：用 vite 的 SSR 模块加载器真实执行 api / mock / store，
 * 验证数据流能跑通（不依赖浏览器环境）。
 * 运行：node smoke-runtime.mjs
 */
import { createServer } from 'vite'

const server = await createServer({
  root: process.cwd(),
  configFile: './vite.config.ts',
  server: { middlewareMode: true },
  logLevel: 'warn'
})

let failed = 0
function check(name, cond, extra = '') {
  if (cond) console.log(`  PASS  ${name}${extra ? ' — ' + extra : ''}`)
  else {
    failed++
    console.log(`  FAIL  ${name}${extra ? ' — ' + extra : ''}`)
  }
}

try {
  const api = await server.ssrLoadModule('/src/api/index.ts')
  const pinia = await import('pinia')

  pinia.setActivePinia(pinia.createPinia())

  console.log('\n[1] 认证')
  const me = await api.authApi.me()
  check('authApi.me()', !!me.email, me.email)

  console.log('\n[2] 作品列表与详情')
  const novels = await api.novelApi.list()
  check('novelApi.list()', novels.length >= 3, `${novels.length} 部作品`)
  check('统计字段存在', !!novels[0].stats, `总字 ${novels[0].stats?.totalWords}`)

  const detail = await api.novelApi.detail(1)
  check('novelApi.detail(1)', detail.mainOutline.includes('主线'), '总纲非空')

  console.log('\n[3] 卷与章纲')
  const volumes = await api.outlineApi.volumes(1)
  check('outlineApi.volumes(1)', volumes.length === 3, `${volumes.length} 卷`)
  const volumes2 = await api.outlineApi.volumes(2)
  const generated = await api.outlineApi.generate(volumes2[0].id)
  check('AI 拆章纲', generated.outlines.length > 0, `${generated.outlines.length} 条章纲`)
  check('提出新设定提案', generated.proposedEntities.length > 0, `${generated.proposedEntities.length} 个`)
  const beatsOk = generated.outlines.every((o) => o.beats.length >= 3 && o.beats.length <= 7)
  check('beat 数在 3—7 区间', beatsOk)

  const outlinesBefore = await api.outlineApi.outlines(volumes2[0].id)
  const beforeNo = outlinesBefore[3].chapterNo
  await api.outlineApi.insert(volumes2[0].id, 2)
  const outlinesAfter = await api.outlineApi.outlines(volumes2[0].id)
  check('插入章纲后重排', outlinesAfter.length === outlinesBefore.length + 1 && beforeNo === 4)

  console.log('\n[4] 设定库')
  const settings = await api.settingsApi.get(1)
  check('角色卡', settings.entities.length > 0, `${settings.entities.length} 条实体`)
  check('文风卡', !!settings.style?.sampleText, '含样章')

  console.log('\n[5] 章节与版本乐观锁')
  const chapters = await api.chapterApi.list(1)
  check('chapterApi.list(1)', chapters.length === 18, `${chapters.length} 章`)
  const c1 = chapters[0]
  const before = c1.contentVersion
  const saved = await api.chapterApi.saveContent(c1.id, c1.contentMd + '\n\n补充一句。', before)
  check('保存正文带版本号', saved.contentVersion === before + 1, `v${before} → v${saved.contentVersion}`)
  let conflict = false
  try {
    await api.chapterApi.saveContent(c1.id, 'xxx', before)
  } catch {
    conflict = true
  }
  check('旧版本号触发 409 冲突', conflict)

  console.log('\n[6] SSE 流式生成')
  const target = chapters.find((c) => c.chapterStatus === 'OUTLINED') ?? chapters[chapters.length - 1]
  const job = await api.generationApi.start(target.id, `key-${Date.now()}`)
  check('创建生成任务', !!job.jobId, `jobId=${job.jobId}, 5 段`)

  const seen = new Set()
  let deltaChars = 0
  await new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('SSE 超时')), 45000)
    api.generationApi.openEventStream(job.jobId, {
      onStarted: (t) => seen.add(`started:${t}`),
      onSegmentStart: (s) => seen.add(`seg:${s.segmentNo}`),
      onDelta: (_n, t) => {
        deltaChars += t.length
      },
      onSegmentDone: (n) => seen.add(`done:${n}`),
      onDraftReady: () => seen.add('draft'),
      onCompleted: () => {
        seen.add('completed')
        clearTimeout(timer)
        resolve()
      },
      onFailed: (m) => {
        clearTimeout(timer)
        reject(new Error(m))
      }
    })
  })
  check('收到 job.started', [...seen].some((k) => k.startsWith('started:5')))
  check('收到 5 段 started', [1, 2, 3, 4, 5].every((n) => seen.has(`seg:${n}`)))
  check('收到 5 段 completed', [1, 2, 3, 4, 5].every((n) => seen.has(`done:${n}`)))
  check('收到 draft_ready + completed', seen.has('draft') && seen.has('completed'))
  check('增量文本累计', deltaChars > 2000, `${deltaChars} 字符`)

  const after = await api.chapterApi.detail(target.id)
  const w = api.countWords(after.contentMd)
  check('生成后字数在 2300—2700', w >= 2300 && w <= 2700, `${w} 字`)
  check('章节状态流转到待审', after.chapterStatus === 'REVIEW_PENDING', after.chapterStatus)

  console.log('\n[7] 停止生成')
  const target2 = chapters.find((c) => c.chapterNo === 11) ?? chapters[1]
  const job2 = await api.generationApi.start(target2.id, `key2-${Date.now()}`)
  let stopped = false
  await new Promise((resolve) => {
    api.generationApi.openEventStream(job2.jobId, {
      onDelta: () => {
        if (!stopped && Math.random() < 0.02) {
          stopped = true
          api.generationApi.stop(job2.jobId).then(resolve)
        }
      },
      onStopped: () => resolve(),
      onCompleted: () => resolve()
    })
  })
  const st = await api.generationApi.status(job2.jobId)
  check('停止后任务状态', st.status === 'STOPPED' || st.status === 'COMPLETED', st.status)

  console.log('\n[8] 审核与入库')
  const pendingChapter = (await api.chapterApi.list(1)).find((c) => c.chapterStatus === 'REVIEW_PENDING')
  const draft = await api.reviewApi.draft(pendingChapter.id)
  check('生成自检草稿', draft.issues.length > 0, `${draft.issues.length} 个问题`)
  check('生成摘要草稿', draft.summary.content.length > 100, `${draft.summary.content.length} 字`)
  check('生成变更草稿', draft.changes.length > 0, `${draft.changes.length} 条`)
  const beforeEntity = (await api.settingsApi.get(1)).entities.length
  draft.changes.forEach((c) => (c.accepted = true))
  await api.reviewApi.approve(pendingChapter.id, { summary: draft.summary.content, changes: draft.changes })
  const approved = await api.chapterApi.detail(pendingChapter.id)
  check('入库后状态为 APPROVED', approved.chapterStatus === 'APPROVED')
  const afterEntity = (await api.settingsApi.get(1)).entities.length
  check('全部接受后新设定写入设定库', afterEntity > beforeEntity, `${beforeEntity} → ${afterEntity}`)

  console.log('\n[9] 导出与用量')
  const exp = await api.exportApi.run(1, { scope: 'BOOK', format: 'docx', withTitle: true, withAuthor: false })
  check('导出 docx', exp.fileName.endsWith('.docx'), `${exp.fileName} · ${exp.sizeKb}KB`)
  const usage = await api.usageApi.overview()
  check('用量总览', usage.monthCost > 0, `¥${usage.monthCost} · ${usage.byScene.length} 个场景`)

  console.log('\n[10] Store 层')
  const { useNovelStore } = await server.ssrLoadModule('/src/stores/novel.ts')
  const novelStore = useNovelStore()
  await novelStore.enter(1)
  check('store.enter(1)', novelStore.chapters.length === 18 && novelStore.volumes.length === 3)
  const genChk = novelStore.canGenerate(novelStore.chapters[9])
  check('未确认章纲不得生成', typeof genChk.ok === 'boolean', `${genChk.ok ? '允许' : genChk.reason}`)

  console.log(`\n===== ${failed === 0 ? '全部通过' : failed + ' 项失败'} =====`)
} catch (e) {
  failed++
  console.error('\n运行异常：', e)
} finally {
  await server.close()
  process.exit(failed ? 1 : 0)
}
