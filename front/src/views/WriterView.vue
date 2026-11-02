<script setup lang="ts">
/** 章节创作页：左章节树 / 中正文编辑 / 右 AI 面板 */
import { computed, onMounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { useGenerationStore } from '@/stores/generation'
import { useUiStore } from '@/stores/ui'
import { chapterApi, reviewApi, countWords } from '@/api'
import type { Chapter } from '@/types'
import AppIcon from '@/components/common/AppIcon.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppProgress from '@/components/common/AppProgress.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AppTextarea from '@/components/common/AppTextarea.vue'
import AiPulse from '@/components/ai/AiPulse.vue'
import StreamText from '@/components/ai/StreamText.vue'

const route = useRoute()
const router = useRouter()
const novel = useNovelStore()
const gen = useGenerationStore()
const ui = useUiStore()

const activeId = ref<number>(0)
const content = ref('')
const baseVersion = ref(1)
const saving = ref(false)
const mode = ref<'read' | 'edit'>('read')
const checking = ref(false)
const issueSummary = ref<string | null>(null)

const readerEl = ref<HTMLElement | null>(null)
const autoScroll = ref(true)

const STATUS: Record<string, { label: string; tone: 'gray' | 'amber' | 'purple' | 'green' | 'blue' }> = {
  OUTLINED: { label: '未生成', tone: 'gray' },
  GENERATING: { label: '生成中', tone: 'purple' },
  REVIEW_PENDING: { label: '待审核', tone: 'amber' },
  APPROVED: { label: '已审核', tone: 'green' }
}

const chapter = computed<Chapter | null>(
  () => novel.chapters.find((c) => c.id === activeId.value) ?? null
)
const outline = computed(() => {
  const c = chapter.value
  if (!c) return null
  return novel.outlines[c.volumeId]?.find((o) => o.chapterNo === c.chapterNo) ?? null
})
const relatedEntities = computed(() => {
  const c = chapter.value
  if (!c) return []
  const names = outline.value?.characters ?? []
  return (novel.settings?.entities ?? []).filter((e) => names.includes(e.name))
})

const words = computed(() => countWords(mode.value === 'edit' ? content.value : gen.mergedText || content.value))
const inRange = computed(() => words.value >= 2300 && words.value <= 2700)
const gate = computed(() => (chapter.value ? novel.canGenerate(chapter.value) : { ok: false, reason: '请先选择章节' }))

/** 当前展示的正文：生成中优先显示流内容 */
const displayText = computed(() => (gen.isRunning ? gen.mergedText : content.value))

async function selectChapter(c: Chapter) {
  if (gen.isRunning) {
    ui.warn('正在生成中', '请先停止或等待生成结束再切换章节')
    return
  }
  activeId.value = c.id
  mode.value = 'read'
  issueSummary.value = null
  await Promise.all([
    novel.outlines[c.volumeId]?.length ? Promise.resolve() : novel.loadOutlines(c.volumeId),
    loadContent()
  ])
}

async function loadContent() {
  if (!activeId.value) return
  const c = await chapterApi.detail(activeId.value)
  content.value = c.contentMd
  baseVersion.value = c.contentVersion
  novel.patchChapter(activeId.value, { contentMd: c.contentMd, wordCount: c.wordCount })
}

async function ensureChapter() {
  const paramId = Number(route.params.chapterId)
  const list = novel.chapters
  if (!list.length) return
  const target =
    (paramId && list.find((c) => c.id === paramId)) ||
    list.find((c) => c.chapterStatus !== 'APPROVED') ||
    list[0]
  activeId.value = target.id
  await novel.loadOutlines(target.volumeId)
  await loadContent()
}

onMounted(async () => {
  await ensureChapter()
  nextTick(scrollBottom)
})

watch(() => route.params.chapterId, async (v) => {
  if (v) {
    activeId.value = Number(v)
    await loadContent()
  }
})

function scrollBottom() {
  if (!autoScroll.value || !readerEl.value) return
  readerEl.value.scrollTo({ top: readerEl.value.scrollHeight, behavior: 'smooth' })
}

watch(() => gen.wordCount, () => nextTick(scrollBottom))

async function startGenerate() {
  if (!chapter.value) return
  if (!gate.value.ok) {
    ui.error('不能开始生成', gate.value.reason)
    return
  }
  mode.value = 'read'
  content.value = ''
  try {
    await gen.start(chapter.value.id)
    novel.patchChapter(chapter.value.id, { chapterStatus: 'GENERATING' })
    ui.info('开始生成', '五段流式生成中，随时可以停止')
  } catch (e) {
    ui.error('启动失败', (e as Error).message)
  }
}

async function stopGenerate() {
  await gen.stop()
  ui.warn('已停止', '保留已生成内容，可从最后完整段继续')
  if (chapter.value) novel.patchChapter(chapter.value.id, { chapterStatus: 'REVIEW_PENDING' })
}

async function resumeGenerate() {
  await gen.resume()
  ui.info('继续生成', '从断点续写')
}

watch(
  () => gen.status,
  (s) => {
    if (s === 'COMPLETED' && chapter.value) {
      content.value = gen.mergedText
      novel.patchChapter(chapter.value.id, {
        chapterStatus: 'REVIEW_PENDING',
        contentMd: gen.mergedText,
        wordCount: gen.wordCount
      })
      baseVersion.value = baseVersion.value + 1
      ui.success('本章生成完成', `共 ${gen.wordCount} 字，去右侧跑一次一致性自检`)
    }
  }
)

async function saveContent() {
  if (!chapter.value) return
  saving.value = true
  try {
    const c = await chapterApi.saveContent(chapter.value.id, content.value, baseVersion.value)
    baseVersion.value = c.contentVersion
    novel.patchChapter(c.id, { contentMd: c.contentMd, wordCount: c.wordCount })
    mode.value = 'read'
    ui.success('已保存', `第 ${c.contentVersion} 版 · ${c.wordCount} 字`)
  } catch (e) {
    const err = e as Error & { code?: number }
    if (err.code === 409) ui.error('版本冲突', '正文已被其他地方更新，请刷新后重新编辑')
    else ui.error('保存失败', err.message)
  } finally {
    saving.value = false
  }
}

async function runCheck() {
  if (!chapter.value) return
  checking.value = true
  ui.showLoading('正在做一致性自检…')
  try {
    const d = await reviewApi.draft(chapter.value.id)
    const high = d.issues.filter((i) => i.level === 'HIGH').length
    issueSummary.value = high
      ? `发现 ${d.issues.length} 个问题，其中 ${high} 个高危`
      : d.issues.length
        ? `发现 ${d.issues.length} 个低/中危问题`
        : '没有发现明显问题'
    if (high) ui.warn('自检完成', issueSummary.value)
    else ui.success('自检完成', issueSummary.value)
    novel.patchChapter(chapter.value.id, { auditStatus: d.issues.length ? 'NEEDS_REVIEW' : 'PASSED' })
  } catch (e) {
    ui.error('自检失败', (e as Error).message)
  } finally {
    ui.hideLoading()
    checking.value = false
  }
}

function goReview() {
  if (!chapter.value) return
  router.push(`/novel/${novel.currentId}/review/${chapter.value.id}`)
}
</script>

<template>
  <div class="writer">
    <!-- 左：章节树 -->
    <aside class="col tree-col">
      <header class="tree-hd">
        <h3>章节</h3>
        <span class="mono">{{ novel.chapterGroups.APPROVED }}/{{ novel.chapters.length }}</span>
      </header>
      <ul class="tree">
        <li
          v-for="(c, i) in novel.chapters"
          :key="c.id"
          class="tree-item"
          :class="{ on: c.id === activeId }"
          :style="{ animationDelay: `${Math.min(i, 14) * 40}ms` }"
          @click="selectChapter(c)"
        >
          <span class="no mono">{{ c.chapterNo }}</span>
          <div class="ti-body">
            <b>{{ c.title.replace(/^\d+\.\s*/, '') }}</b>
            <div class="ti-tags">
              <AppTag :label="STATUS[c.chapterStatus]?.label" :tone="STATUS[c.chapterStatus]?.tone" size="xs" dot />
              <span v-if="c.wordCount" class="mono">{{ c.wordCount }} 字</span>
            </div>
          </div>
        </li>
        <AppEmpty v-if="!novel.chapters.length" title="还没有章节" desc="先去大纲页确认本卷章纲" icon="file" />
      </ul>
    </aside>

    <!-- 中：正文 -->
    <section class="col main-col">
      <header class="editor-bar">
        <div class="eb-left">
          <AiPulse :active="gen.isRunning" :size="34" />
          <div>
            <h2>{{ chapter ? `第 ${chapter.chapterNo} 章 · ${chapter.title.replace(/^\d+\.\s*/, '')}` : '未选择章节' }}</h2>
            <div class="eb-meta">
              <span class="mono">{{ words }} 字</span>
              <AppTag v-if="words" :label="inRange ? '字数达标' : '字数在区间外'" :tone="inRange ? 'green' : 'amber'" size="xs" />
              <AppTag v-if="chapter" :label="`第 ${baseVersion} 版`" tone="gray" size="xs" />
              <span class="range">验收区间 2300—2700</span>
            </div>
          </div>
        </div>

        <div class="eb-right">
          <AppButton v-if="!gen.isRunning && gen.status === 'STOPPED'" size="sm" variant="primary" icon="play" @click="resumeGenerate">
            继续生成
          </AppButton>
          <AppButton v-if="gen.isRunning" size="sm" variant="danger" icon="stop" @click="stopGenerate">
            停止
          </AppButton>
          <AppButton
            v-else
            size="sm"
            variant="primary"
            icon="sparkle"
            @click="startGenerate"
          >
            {{ chapter?.wordCount ? '重新生成' : '生成本章' }}
          </AppButton>
        </div>
      </header>

      <!-- 生成进度条 -->
      <div v-if="gen.segments.length" class="seg-bar">
        <div v-for="s in gen.segments" :key="s.segmentNo" class="seg" :class="`st-${s.status.toLowerCase()}`">
          <div class="seg-hd">
            <span>第 {{ s.segmentNo }} 段</span>
            <span class="mono">{{ s.written }}/{{ s.targetWords }}</span>
          </div>
          <AppProgress :value="s.written" :max="Math.max(1, s.targetWords)" :height="5" striped />
        </div>
      </div>

      <div class="surface-top">
        <div class="tools">
          <div class="switch-group">
            <button :class="{ on: mode === 'read' }" @click="mode = 'read'">
              <AppIcon name="eye" :size="14" /> 阅读
            </button>
            <button :class="{ on: mode === 'edit' }" @click="mode = 'edit'" :disabled="gen.isRunning">
              <AppIcon name="edit" :size="14" /> 编辑
            </button>
          </div>
          <label class="ck"><input v-model="autoScroll" type="checkbox" /> 自动滚动</label>
          <div class="grow" />
          <AppButton v-if="mode === 'edit'" size="sm" variant="primary" icon="check" :loading="saving" @click="saveContent">
            保存（v{{ baseVersion }} → v{{ baseVersion + 1 }}）
          </AppButton>
        </div>
      </div>

      <div ref="readerEl" class="doc-area">
        <StreamText
          v-if="mode === 'read'"
          :content="displayText"
          :typing="gen.isRunning"
          serif
          :font-size="16"
          :line-height="2.0"
          :min-height="420"
        />
        <AppTextarea
          v-else
          v-model="content"
          :rows="26"
          mono
          show-count
          placeholder="在这里直接改 Markdown 正文。保存时带 expectedVersion，冲突会提示。"
        />
      </div>

      <footer class="editor-foot">
        <span v-if="gen.isRunning" class="live">
          <AiPulse :active="true" :size="16" />
          正在写第 {{ gen.currentSegment }}/{{ gen.totalSegments }} 段 · {{ gen.wordCount }} 字 · {{ gen.percent }}%
        </span>
        <span v-else class="live dim">
          {{ chapter?.wordCount ? '正文已就绪，可以自检或去审核' : '还没有正文' }}
        </span>
        <div class="grow" />
        <AppButton variant="outline" size="sm" icon="shield" :loading="checking" @click="runCheck">
          一致性自检
        </AppButton>
        <AppButton variant="primary" size="sm" icon="arrowRight" @click="goReview">去审核入库</AppButton>
      </footer>
    </section>

    <!-- 右：AI 面板 -->
    <aside class="col side-col">
      <AppEmpty v-if="!chapter" title="选择一章开始" icon="file" />

      <template v-else>
        <!-- 本章章纲 -->
        <section class="side-card">
          <h4><AppIcon name="target" :size="14" /> 本章章纲</h4>
          <ol v-if="outline" class="beats">
            <li v-for="(b, i) in outline.beats" :key="i">{{ b }}</li>
          </ol>
          <p v-else class="muted">未找到对应章纲（可能是删除后残留）</p>
        </section>

        <!-- 引用设定 -->
        <section class="side-card">
          <h4><AppIcon name="users" :size="14" /> 本章引用设定</h4>
          <div class="ents">
            <details v-for="e in relatedEntities" :key="e.id">
              <summary>
                <span class="dot" aria-hidden="true" />
                {{ e.name }}
                <AppTag
                  :label="e.approvalStatus === 'CONFIRMED' ? '已确认' : '草稿'"
                  :tone="e.approvalStatus === 'CONFIRMED' ? 'green' : 'amber'"
                  size="xs"
                />
              </summary>
              <dl>
                <div v-for="(v, k) in e.card" :key="k"><dt>{{ k }}</dt><dd>{{ v }}</dd></div>
              </dl>
            </details>
            <p v-if="!relatedEntities.length" class="muted">章纲里没有指定出场角色</p>
          </div>
        </section>

        <!-- 自检结果摘要 -->
        <section class="side-card">
          <h4><AppIcon name="shield" :size="14" /> 最近自检</h4>
          <p v-if="!issueSummary" class="muted">还没跑过自检。写完草本后点底部「一致性自检」。</p>
          <p v-else class="res">{{ issueSummary }}</p>
        </section>

        <!-- 字数 -->
        <section class="side-card">
          <h4><AppIcon name="book" :size="14" /> 字数校验</h4>
          <div class="wc">
            <AppProgress
              type="ring"
              :size="58"
              :thickness="6"
              :value="Math.min(100, Math.round((words / 2500) * 100))"
              :tone="inRange ? '34,197,94' : '245,158,11'"
            >
              <b class="mono">{{ words }}</b>
            </AppProgress>
            <div class="wc-meta">
              <p>目标 2500 字</p>
              <p class="dim">区间 2300—2700</p>
              <p :class="{ ok: inRange, bad: !inRange }">{{ inRange ? '在合格区间' : '不在合格区间' }}</p>
            </div>
          </div>
        </section>

        <!-- 前置校验 -->
        <section v-if="!gate.ok" class="side-card gate">
          <h4><AppIcon name="warning" :size="14" /> 当前不可生成</h4>
          <p>{{ gate.reason }}</p>
        </section>
      </template>
    </aside>
  </div>
</template>

<style scoped>
.writer {
  display: grid;
  grid-template-columns: 234px minmax(0, 1fr) 300px;
  height: 100%;
  min-height: 0;
}

.col {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 左 */
.tree-col {
  background: var(--surface);
  border-right: 1px solid var(--line);
}
.tree-hd {
  height: 44px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border-bottom: 1px solid var(--line);
}
.tree-hd h3 {
  font-size: 13px;
  font-weight: 700;
}
.tree-hd span {
  font-size: 11.5px;
  color: var(--text-3);
}
.tree {
  flex: 1;
  overflow: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.tree-item {
  display: grid;
  grid-template-columns: 24px 1fr;
  gap: 9px;
  padding: 8px 9px;
  border-radius: var(--r-sm);
  cursor: pointer;
  animation: fade-up var(--dur-base) var(--ease-out) both;
  transition: background var(--dur-fast) var(--ease-out), transform var(--dur-fast) var(--ease-out);
}
.tree-item:hover {
  background: var(--surface-3);
  transform: translateX(2px);
}
.tree-item.on {
  background: rgba(99, 102, 241, 0.1);
}
.no {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-3);
  background: var(--surface-3);
}
.tree-item.on .no {
  background: var(--grad-brand);
  color: #fff;
  box-shadow: var(--sh-brand);
}
.ti-body b {
  font-size: 12.8px;
  font-weight: 600;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ti-tags {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 3px;
  font-size: 10.5px;
  color: var(--text-4);
}

/* 中 */
.main-col {
  background: var(--bg);
  position: relative;
}
.editor-bar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 11px 18px;
  background: var(--surface);
  border-bottom: 1px solid var(--line);
}
.eb-left {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 0;
}
.eb-left h2 {
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.eb-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: var(--text-3);
  margin-top: 2px;
}
.range {
  color: var(--text-4);
}
.eb-right {
  display: flex;
  gap: 8px;
  flex: none;
}

.seg-bar {
  flex: none;
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  gap: 10px;
  padding: 11px 18px;
  background: linear-gradient(180deg, #fbfbff, var(--surface));
  border-bottom: 1px solid var(--line);
  animation: fade-up var(--dur-base) var(--ease-out) both;
}
.seg-hd {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: var(--text-3);
  margin-bottom: 4px;
  font-weight: 600;
}
.seg.st-completed :deep(.track) {
  background: rgba(52, 211, 153, 0.14);
}
.seg.st-completed .seg-hd {
  color: var(--c-success);
}
.seg.st-running .seg-hd {
  color: var(--brand-strong);
  animation: caret-blink 1.2s ease-in-out infinite;
}

.surface-top {
  flex: none;
  padding: 9px 18px 0;
}
.tools {
  display: flex;
  align-items: center;
  gap: 10px;
}
.switch-group {
  display: inline-flex;
  padding: 3px;
  gap: 2px;
  background: var(--surface-3);
  border: 1px solid var(--line);
  border-radius: var(--r-md);
}
.switch-group button {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 26px;
  padding: 0 11px;
  border-radius: calc(var(--r-md) - 3px);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-3);
  transition: all var(--dur-fast) var(--ease-out);
}
.switch-group button.on {
  background: var(--surface);
  color: var(--brand-strong);
  box-shadow: var(--sh-xs);
}
.switch-group button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.ck {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--text-3);
  cursor: pointer;
}
.ck input {
  accent-color: var(--brand);
}

.doc-area {
  flex: 1;
  overflow: auto;
  padding: 16px 22px 30px;
}

.doc-area :deep(.reader) {
  max-width: 760px;
  margin: 0 auto;
  padding: 26px 30px;
  background: var(--surface);
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  box-shadow: var(--sh-sm);
}

.editor-foot {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 18px;
  background: var(--surface);
  border-top: 1px solid var(--line);
}
.live {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--brand-strong);
}
.live.dim {
  color: var(--text-3);
  font-weight: 500;
}

/* 右 */
.side-col {
  background: var(--surface);
  border-left: 1px solid var(--line);
  padding: 12px;
  gap: 12px;
  overflow: auto;
}

.side-card {
  padding: 13px 14px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: var(--surface-2);
  animation: fade-up var(--dur-base) var(--ease-out) both;
}
.side-card h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-2);
  margin-bottom: 9px;
}
.side-card.gate {
  border-color: rgba(245, 158, 11, 0.4);
  background: rgba(245, 158, 11, 0.07);
}
.side-card.gate h4 {
  color: var(--c-warn);
}
.side-card.gate p {
  font-size: 12px;
  color: var(--text-2);
}

.beats {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.beats li {
  position: relative;
  padding-left: 14px;
  font-size: 12.3px;
  color: var(--text-2);
  line-height: 1.6;
}
.beats li::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 8px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--grad-brand);
}

.muted {
  font-size: 12px;
  color: var(--text-4);
}
.res {
  font-size: 12.3px;
  color: var(--text-2);
  line-height: 1.7;
}

.ents details {
  border-bottom: 1px dashed var(--line-2);
  padding: 5px 0;
  animation: fade-in var(--dur-base) var(--ease-out) both;
}
.ents details:last-child {
  border-bottom: none;
}
.ents summary {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  color: var(--text-1);
  list-style: none;
}
.ents summary::-webkit-details-marker {
  display: none;
}
.ents .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--purple);
}
.ents dl {
  margin: 7px 0 4px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 12px;
  border-left: 2px solid rgba(99, 102, 241, 0.18);
}
.ents dl > div {
  display: grid;
  grid-template-columns: 58px 1fr;
  gap: 6px;
  font-size: 11.8px;
}
.ents dt {
  color: var(--text-3);
}
.ents dd {
  margin: 0;
  color: var(--text-2);
  line-height: 1.55;
}

.wc {
  display: flex;
  align-items: center;
  gap: 13px;
}
.wc b {
  font-size: 15px;
  font-weight: 800;
  color: var(--text-1);
}
.wc-meta p {
  font-size: 11.8px;
  color: var(--text-3);
}
.wc-meta .dim {
  color: var(--text-4);
}
.wc-meta .ok {
  color: var(--c-success);
  font-weight: 700;
}
.wc-meta .bad {
  color: var(--c-warn);
  font-weight: 700;
}

@media (max-width: 1240px) {
  .writer {
    grid-template-columns: 200px minmax(0, 1fr) 260px;
  }
}
</style>
