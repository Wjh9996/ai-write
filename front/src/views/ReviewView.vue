<script setup lang="ts">
/** 审核入库页：正文 + 问题定位 + 摘要/变更草稿 + 通过入库 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { useUiStore } from '@/stores/ui'
import { reviewApi, countWords } from '@/api'
import type { Issue, ReviewDraft } from '@/types'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppTextarea from '@/components/common/AppTextarea.vue'
import AppSkeleton from '@/components/common/AppSkeleton.vue'
import StreamText from '@/components/ai/StreamText.vue'

const route = useRoute()
const router = useRouter()
const novel = useNovelStore()
const ui = useUiStore()

const activeId = ref(0)
const chapter = computed(() => novel.chapters.find((c) => c.id === activeId.value) ?? null)
const draft = ref<ReviewDraft | null>(null)
const loading = ref(false)
const approving = ref(false)
const summaryText = ref('')
const noteOpen = ref(false)
const rejectReason = ref('')

const LEVEL: Record<string, { tone: 'rose' | 'amber' | 'blue'; label: string }> = {
  HIGH: { tone: 'rose', label: '高危' },
  MID: { tone: 'amber', label: '中危' },
  LOW: { tone: 'blue', label: '低危' }
}

const openCount = computed(() => (draft.value?.issues ?? []).filter((i) => !i.resolved).length)
const highCount = computed(
  () => (draft.value?.issues ?? []).filter((i) => i.level === 'HIGH' && !i.resolved).length
)
const acceptedCount = computed(() => (draft.value?.changes ?? []).filter((c) => c.accepted).length)
const words = computed(() => countWords(chapter.value?.contentMd ?? ''))
const canApprove = computed(() => highCount.value === 0 && !!summaryText.value.trim())

const pending = computed(() =>
  novel.chapters.filter((c) => c.chapterStatus === 'REVIEW_PENDING' || c.chapterStatus === 'GENERATING')
)

async function ensureChapter() {
  const param = Number(route.params.chapterId)
  if (!novel.chapters.length) {
    await novel.loadChapters()
  }
  const target = (param && novel.chapters.find((c) => c.id === param)) || pending.value[0] || novel.chapters[0]
  if (!target) return
  activeId.value = target.id
  await loadDraft(true)
}

async function loadDraft(silent = false) {
  if (!chapter.value) return
  loading.value = !silent
  draft.value = null
  try {
    draft.value = await reviewApi.draft(chapter.value.id)
    summaryText.value = draft.value.summary.content
  } catch (e) {
    if (!silent) ui.error('加载失败', (e as Error).message)
  } finally {
    loading.value = false
  }
}

async function regenerate(index: number) {
  if (!draft.value || !chapter.value) return
  const it = draft.value.issues[index]
  it.resolved = true
  ui.info('已标记为已处理', `「${it.type}」问题需要你在正文里手动修订`)
}

function focusQuote(it: Issue) {
  ui.info('已复制待查片段', it.quote)
  navigator.clipboard?.writeText(it.quote).catch(() => {})
}

function toggleChange(idx: number) {
  const ch = draft.value?.changes[idx]
  if (ch) {
    ch.accepted = !ch.accepted
    ch.edited = true
  }
}

async function approve() {
  if (!chapter.value || !draft.value) return
  if (!canApprove.value) {
    ui.error('还不能入库', highCount.value ? '还有高危问题没处理完' : '摘要不能为空')
    return
  }
  approving.value = true
  ui.showLoading('正在入库（摘要 + 角色状态 + 时间线 + 伏笔 同一事务）…')
  try {
    await reviewApi.approve(chapter.value.id, {
      summary: summaryText.value,
      changes: draft.value.changes
    })
    novel.patchChapter(chapter.value.id, { chapterStatus: 'APPROVED', auditStatus: 'PASSED' })
    await novel.loadChapters()
    ui.success('已入库', `第 ${chapter.value.chapterNo} 章转为「已审核」，下一章现在可以生成了`)
    router.push(`/novel/${novel.currentId}/write/${chapter.value.id + 1}`)
  } catch (e) {
    ui.error('入库失败', (e as Error).message)
  } finally {
    ui.hideLoading()
    approving.value = false
  }
}

async function reject() {
  if (!chapter.value) return
  await reviewApi.reject(chapter.value.id, rejectReason.value)
  novel.patchChapter(chapter.value.id, { chapterStatus: 'GENERATING' })
  noteOpen.value = false
  ui.warn('已退回', '章节回到「生成中」，改完后可以重新自检')
}

onMounted(ensureChapter)
watch(() => route.params.chapterId, ensureChapter)
watch(activeId, () => loadDraft(true))
</script>

<template>
  <div class="page">
    <nav class="chips-bar">
      <span class="lb">待处理章节</span>
      <button
        v-for="c in pending"
        :key="c.id"
        class="chip"
        :class="{ on: c.id === activeId }"
        @click="activeId = c.id"
      >
        第 {{ c.chapterNo }} 章
      </button>
      <span v-if="!pending.length" class="none">当前没有待审核的章节</span>
      <div class="grow" />
      <AppButton size="sm" variant="outline" icon="refresh" :loading="loading" @click="loadDraft()">
        重新自检
      </AppButton>
    </nav>

    <AppEmpty v-if="!chapter" title="还没有章节" desc="先去创作页写一章再回来审核" icon="review">
      <AppButton variant="primary" icon="arrowRight" @click="router.push(`/novel/${novel.currentId}/write`)">
        去创作
      </AppButton>
    </AppEmpty>

    <div v-else class="grid2">
      <!-- 左：正文 -->
      <AppCard :title="`第 ${chapter.chapterNo} 章 · ${chapter.title.replace(/^\d+\.\s*/, '')}`" icon="book" tone="ocean">
        <template #actions>
          <AppTag :label="`${words} 字`" tone="blue" size="xs" />
          <AppTag
            :label="chapter.auditStatus === 'PASSED' ? '自检通过' : '有问题待处理'"
            :tone="chapter.auditStatus === 'PASSED' ? 'green' : 'amber'"
            size="xs"
            dot
          />
        </template>

        <div v-if="loading" class="pad"><AppSkeleton :lines="8" /></div>
        <div v-else class="doc">
          <StreamText :content="chapter.contentMd" serif :font-size="15" :line-height="1.95" :min-height="360" />
        </div>
      </AppCard>

      <!-- 右：审核面板 -->
      <div class="side">
        <!-- 自检问题 -->
        <AppCard title="一致性自检" :desc="`${openCount} 条未处理，${highCount} 条高危`" icon="shield" tone="sunset">
          <template #actions>
            <AppTag v-if="highCount" label="有高危" tone="rose" size="xs" dot />
            <AppTag v-else-if="!openCount" label="全部处理" tone="green" size="xs" />
          </template>

          <div v-if="loading" class="pad"><AppSkeleton :lines="4" /></div>
          <AppEmpty
            v-else-if="!draft?.issues?.length"
            title="没有发现问题"
            desc="角色白名单、时间线、视角、章纲越界都在范围内"
            icon="shield"
          />
          <ul v-else class="issues">
            <li v-for="(it, i) in draft.issues" :key="it.id" :class="{ resolved: it.resolved }">
              <div class="ih">
                <AppTag :label="it.type" tone="purple" size="xs" />
                <AppTag :label="LEVEL[it.level].label" :tone="LEVEL[it.level].tone" size="xs" />
                <div class="grow" />
                <button class="mn" title="复制引文" @click="focusQuote(it)"><AppIcon name="search" :size="14" /></button>
                <button class="mn" title="标记已处理" @click="regenerate(i)"><AppIcon name="check" :size="14" /></button>
              </div>
              <blockquote>{{ it.quote }}</blockquote>
              <p class="is">{{ it.issue }}</p>
              <p class="sg"><AppIcon name="think" :size="12" /> {{ it.suggest }}</p>
              <span v-if="it.resolved" class="ribbon">已处理</span>
            </li>
          </ul>
        </AppCard>

        <!-- 章摘要 -->
        <AppCard title="章摘要草稿" desc="200—300 字，只依据当前正文生成，确认后才入库" icon="clipboard" tone="brand">
          <AppTextarea v-model="summaryText" :rows="6" placeholder="生成摘要后可以直接手动改" show-count />
          <p class="src mono">
            source_content_version: {{ draft?.summary.sourceContentVersion ?? '-' }} · 当前正文版本
            {{ chapter.contentVersion }}
          </p>
        </AppCard>

        <!-- 设定变更 -->
        <AppCard title="设定变更草稿" desc="勾选的条目才会写入下一章上下文" icon="bolt" tone="mint">
          <template #actions>
            <AppTag :label="`已选 ${acceptedCount}/${draft?.changes?.length ?? 0}`" tone="teal" size="xs" />
          </template>

          <div v-if="loading" class="pad"><AppSkeleton :lines="3" /></div>
          <ul v-else-if="draft?.changes?.length" class="changes">
            <li v-for="(ch, i) in draft.changes" :key="ch.id" :class="{ on: ch.accepted }">
              <button class="cbx" @click="toggleChange(i)">
                <AppIcon v-if="ch.accepted" name="check" :size="12" color="#fff" />
              </button>
              <div class="ct">
                <b>
                  {{ ch.name }}
                  <AppTag
                    :label="
                      ch.kind === 'TIMELINE'
                        ? '时间线'
                        : ch.kind === 'FORESHADOWING'
                          ? `伏笔 · ${ch.action ?? ''}`
                          : ch.kind === 'NEW_ENTITY'
                            ? '新增设定'
                            : '角色状态'
                    "
                    :tone="ch.kind === 'NEW_ENTITY' ? 'pink' : ch.kind === 'FORESHADOWING' ? 'amber' : 'teal'"
                    size="xs"
                  />
                </b>
                <p>{{ ch.change }}</p>
                <small v-if="ch.time">{{ ch.time }}</small>
              </div>
            </li>
          </ul>
          <AppEmpty v-else title="没有检测到设定变化" icon="bolt" />
        </AppCard>

        <!-- 操作 -->
        <div class="actions">
          <AppButton variant="ghost" icon="arrowLeft" @click="noteOpen = true">退回修改</AppButton>
          <AppButton
            variant="primary"
            size="lg"
            icon="shield"
            :loading="approving"
            :disabled="!canApprove"
            @click="approve"
          >
            通过并入库
          </AppButton>
        </div>
        <p v-if="!canApprove" class="tip">
          {{ highCount ? `还有 ${highCount} 条高危问题需要处理` : '摘要不能为空' }}
        </p>
      </div>
    </div>

    <AppModal v-model="noteOpen" title="退回修改" desc="写明退回原因，章节会回到「生成中」" icon="arrowLeft" :width="440">
      <AppTextarea v-model="rejectReason" :rows="5" placeholder="例如：第一章不能提「净息符」，删掉相关描写" />
      <template #footer>
        <AppButton variant="ghost" @click="noteOpen = false">取消</AppButton>
        <AppButton variant="danger" icon="check" @click="reject">确认退回</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.page {
  padding: 20px 24px 60px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.chips-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  animation: fade-up 380ms var(--ease-out) both;
}
.lb {
  font-size: 12px;
  color: var(--text-3);
  font-weight: 600;
}
.chip {
  height: 28px;
  padding: 0 12px;
  border-radius: var(--r-full);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  background: var(--surface);
  border: 1px solid var(--line-2);
  transition: all var(--dur-fast) var(--ease-out);
}
.chip:hover {
  border-color: var(--brand);
  color: var(--brand-strong);
  transform: translateY(-1px);
}
.chip.on {
  background: var(--grad-brand);
  color: #fff;
  border-color: transparent;
  box-shadow: var(--sh-brand);
}
.none {
  font-size: 12px;
  color: var(--text-4);
}

.grid2 {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 16px;
  align-items: start;
}
@media (max-width: 1180px) {
  .grid2 {
    grid-template-columns: 1fr;
  }
}

.pad {
  padding: 8px 0;
}
.doc {
  max-height: 640px;
  overflow: auto;
  padding: 18px 22px;
  border-radius: var(--r-md);
  background: var(--surface-2);
  border: 1px solid var(--line);
}

.side {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.issues {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.issues li {
  position: relative;
  padding: 11px 12px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: var(--surface-2);
  overflow: hidden;
  animation: fade-up var(--dur-base) var(--ease-out) both;
  transition: all var(--dur-base) var(--ease-out);
}
.issues li:hover {
  border-color: var(--line-2);
  box-shadow: var(--sh-sm);
}
.issues li.resolved {
  opacity: 0.55;
}

.ih {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 7px;
}
.mn {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  color: var(--text-3);
  transition: all var(--dur-fast) var(--ease-out);
}
.mn:hover {
  background: rgba(99, 102, 241, 0.1);
  color: var(--brand-strong);
}

.issues blockquote {
  margin: 0 0 6px;
  padding: 6px 10px;
  border-left: 3px solid var(--pink);
  background: rgba(236, 72, 153, 0.05);
  border-radius: 0 7px 7px 0;
  font-size: 12.3px;
  color: var(--text-2);
  line-height: 1.6;
}
.is {
  font-size: 12.3px;
  color: var(--text-1);
  line-height: 1.6;
}
.sg {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin-top: 5px;
  font-size: 12px;
  color: var(--brand-strong);
  line-height: 1.6;
}
.sg svg {
  flex: none;
  margin-top: 3px;
}

.ribbon {
  position: absolute;
  top: 8px;
  right: -26px;
  transform: rotate(38deg);
  background: var(--c-success);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 28px;
}

.src {
  margin-top: 7px;
  font-size: 10.8px;
  color: var(--text-4);
}

.changes {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.changes li {
  display: grid;
  grid-template-columns: 20px 1fr;
  gap: 9px;
  padding: 9px 10px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line);
  background: var(--surface-2);
  transition: all var(--dur-base) var(--ease-out);
}
.changes li.on {
  border-color: rgba(52, 211, 153, 0.45);
  background: rgba(52, 211, 153, 0.06);
}
.cbx {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  border: 1.5px solid var(--line-strong);
  display: grid;
  place-items: center;
  background: var(--surface);
  transition: all var(--dur-fast) var(--ease-spring);
}
.changes li.on .cbx {
  background: linear-gradient(135deg, #34d399, #10b981);
  border-color: transparent;
}
.ct b {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.8px;
}
.ct p {
  font-size: 12px;
  color: var(--text-2);
  line-height: 1.6;
  margin-top: 2px;
}
.ct small {
  font-size: 11px;
  color: var(--text-4);
}

.actions {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 14px;
  border-radius: var(--r-lg);
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: var(--sh-sm);
}
.tip {
  font-size: 11.5px;
  color: var(--c-warn);
  text-align: center;
  margin-top: -6px;
}
</style>
