<script setup lang="ts">
/** 卷纲确认页：左事件序列 / 右 AI 拆出的章纲列表 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { useUiStore } from '@/stores/ui'
import { outlineApi } from '@/api'
import type { ChapterOutline, Volume } from '@/types'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppTextarea from '@/components/common/AppTextarea.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AppProgress from '@/components/common/AppProgress.vue'
import AppSkeleton from '@/components/common/AppSkeleton.vue'

const route = useRoute()
const router = useRouter()
const novel = useNovelStore()
const ui = useUiStore()

const volumeId = computed(() => Number(route.params.volumeId))
const volume = ref<Volume | null>(null)
const outlines = ref<ChapterOutline[]>([])
const loading = ref(false)

const eventsText = ref('')
const editingEvents = ref(false)
const savingEvents = ref(false)

const editOpen = ref(false)
const editing = ref<ChapterOutline | null>(null)
const editForm = ref({ title: '', beatsText: '', characters: '', words: 2500 })
const wordsText = ref('2500')

const dragIndex = ref(-1)
const overIndex = ref(-1)

const confirmedCount = computed(() => outlines.value.filter((o) => o.status === 'CONFIRMED').length)
const confirmProgress = computed(() =>
  outlines.value.length ? Math.round((confirmedCount.value / outlines.value.length) * 100) : 0
)
const totalWords = computed(() => outlines.value.reduce((a, o) => a + o.estimatedWords, 0))

async function load() {
  loading.value = true
  try {
    const vs = await outlineApi.volumes(novel.currentId)
    volume.value = vs.find((v) => v.id === volumeId.value) ?? null
    outlines.value = (await novel.loadOutlines(volumeId.value)) ?? []
    eventsText.value = (volume.value?.events ?? []).map((e) => e.content).join('\n')
  } catch (e) {
    ui.error('加载失败', (e as Error).message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(volumeId, load)

async function generate() {
  if (!volume.value) return
  ui.showLoading('AI 正在按本卷事件拆章纲…')
  try {
    const res = await outlineApi.generate(volumeId.value)
    outlines.value = res.outlines
    await syncVolume()
    ui.success('章纲已生成', `${res.outlines.length} 条待确认`)
  } catch (e) {
    ui.error('失败', (e as Error).message)
  } finally {
    ui.hideLoading()
  }
}

async function syncVolume() {
  const vs = await outlineApi.volumes(novel.currentId)
  volume.value = vs.find((v) => v.id === volumeId.value) ?? null
}

async function saveEvents() {
  savingEvents.value = true
  try {
    const list = eventsText.value
      .split('\n')
      .map((l) => l.trim())
      .filter(Boolean)
    await outlineApi.saveEvents(volumeId.value, list)
    await syncVolume()
    editingEvents.value = false
    ui.success('阶段事件已保存', `${list.length} 条`)
  } finally {
    savingEvents.value = false
  }
}

function openEdit(o: ChapterOutline) {
  editing.value = o
  editForm.value = {
    title: o.title.replace(/^\d+\.\s*/, ''),
    beatsText: o.beats.join('\n'),
    characters: o.characters.join('、'),
    words: o.estimatedWords
  }
  wordsText.value = String(o.estimatedWords)
  editOpen.value = true
}

async function submitEdit() {
  if (!editing.value) return
  const beats = editForm.value.beatsText
    .split('\n')
    .map((b) => b.trim())
    .filter(Boolean)
  if (beats.length < 3 || beats.length > 7) {
    ui.warn('beat 数量需要在 3—7 条之间', `当前 ${beats.length} 条`)
    return
  }
  const characters = editForm.value.characters
    .split(/[、,，]/)
    .map((c) => c.trim())
    .filter(Boolean)

  try {
    await outlineApi.update(editing.value.id, {
      title: `${editing.value.chapterNo}. ${editForm.value.title}`,
      beats,
      characters,
      estimatedWords: Number(wordsText.value) || 2500
    })
    outlines.value = await outlineApi.outlines(volumeId.value)
    editOpen.value = false
    ui.success('章纲已更新')
  } catch (e) {
    ui.error('保存失败', (e as Error).message)
  }
}

async function removeOutline(o: ChapterOutline) {
  await outlineApi.remove(o.id)
  outlines.value = outlines.value.filter((x) => x.id !== o.id)
  ui.info('已删除该章纲')
}

async function insertAfter(o: ChapterOutline) {
  await outlineApi.insert(volumeId.value, o.chapterNo)
  outlines.value = await outlineApi.outlines(volumeId.value)
  ui.success('已插入新章纲')
}

// 拖拽排序
function onDragStart(i: number, ev: DragEvent) {
  dragIndex.value = i
  ;(ev.currentTarget as HTMLElement).classList.add('dragging')
  ev.dataTransfer?.setData('text/plain', String(i))
}
function onDragOver(i: number, ev: DragEvent) {
  ev.preventDefault()
  overIndex.value = i
}
function onDrop(i: number) {
  if (dragIndex.value < 0 || dragIndex.value === i) return resetDrag()
  const arr = [...outlines.value]
  const [moved] = arr.splice(dragIndex.value, 1)
  arr.splice(i, 0, moved)
  outlines.value = arr.map((o, idx) => ({ ...o, chapterNo: idx + 1 }))
  persistOrder()
}
function resetDrag() {
  dragIndex.value = -1
  overIndex.value = -1
  document.querySelectorAll('.dragging').forEach((el) => el.classList.remove('dragging'))
}

async function persistOrder() {
  await outlineApi.reorder(
    volumeId.value,
    outlines.value.map((o) => ({ id: o.id, chapterNo: o.chapterNo }))
  )
  ui.success('顺序已更新')
}

async function confirmAll() {
  if (!outlines.value.length) {
    ui.warn('还没有可确认的章纲')
    return
  }
  const badBeats = outlines.value.filter((o) => o.beats.length < 3 || o.beats.length > 7)
  if (badBeats.length) {
    ui.error('有章纲不合规', `${badBeats.map((o) => o.chapterNo).join('、')} 章的 beat 数不在 3—7 条`)
    return
  }
  ui.showLoading('正在确认并生成章节占位…')
  try {
    await outlineApi.confirm(volumeId.value)
    await syncVolume()
    outlines.value = await outlineApi.outlines(volumeId.value)
    await novel.loadChapters()
    ui.success('本卷章纲已确认', `${outlines.value.length} 章已加入章节列表，状态为「未生成」`)
  } catch (e) {
    ui.error('确认失败', (e as Error).message)
  } finally {
    ui.hideLoading()
  }
}

function goWriter() {
  router.push(`/novel/${novel.currentId}/write`)
}
</script>

<template>
  <div class="page">
    <nav class="crumb">
      <button @click="router.push(`/novel/${novel.currentId}/outline`)">
        <AppIcon name="arrowLeft" :size="14" /> 返回大纲
      </button>
      <span>/</span>
      <b>{{ volume?.title ?? '卷' }}</b>
    </nav>

    <div class="cols">
      <!-- 左：卷事件 -->
      <AppCard class="col-left" title="本卷阶段事件" desc="一行一个事件，AI 会按顺序把它们拆成章" icon="target" tone="ocean">
        <template #actions>
          <AppButton v-if="!editingEvents" size="sm" variant="outline" icon="edit" @click="editingEvents = true">
            编辑
          </AppButton>
          <AppButton v-else size="sm" variant="primary" icon="check" :loading="savingEvents" @click="saveEvents">
            保存
          </AppButton>
        </template>

        <AppTextarea
          v-if="editingEvents"
          v-model="eventsText"
          :rows="14"
          placeholder="一行一条事件，例如：&#10;在矿洞深处发现一枚无人认领的灰蛋&#10;尝试孵化失败三次&#10;用体温把蛋孵了出来"
        />
        <ol v-else class="events">
          <li v-for="(e, i) in volume?.events ?? []" :key="e.id" :style="{ animationDelay: `${i * 45}ms` }">
            <span class="dot">
              <em>{{ i + 1 }}</em>
            </span>
            <span class="tx">{{ e.content }}</span>
          </li>
          <AppEmpty
            v-if="!volume?.events?.length"
            title="还没写阶段事件"
            desc="把这一卷要发生的事按顺序列出来，AI 会照着拆章"
            icon="target"
          >
            <AppButton size="sm" variant="primary" icon="edit" @click="editingEvents = true">开始编写</AppButton>
          </AppEmpty>
        </ol>
      </AppCard>

      <!-- 右：章纲列表 -->
      <AppCard class="col-right" tone="violet" icon="layers">
        <template #header>
          <div class="rh">
            <div>
              <h3>AI 拆出的章纲</h3>
              <p>每条 3—7 个 beat，确认后才会进入创作</p>
            </div>
            <div class="rstat">
              <AppProgress type="ring" :size="42" :thickness="4" :value="confirmProgress" :show-label="true" />
              <div class="rt">
                <b class="mono">{{ confirmedCount }}/{{ outlines.length }}</b>
                <small>已确认 · 预计 {{ totalWords.toLocaleString('zh-CN') }} 字</small>
              </div>
            </div>
          </div>
        </template>
        <template #actions>
          <AppButton size="sm" variant="outline" icon="refresh" @click="generate">重新生成</AppButton>
          <AppButton size="sm" variant="primary" icon="check" @click="confirmAll">确认本卷</AppButton>
        </template>

        <div v-if="loading">
          <AppSkeleton :lines="5" />
        </div>

        <AppEmpty
          v-else-if="!outlines.length"
          title="还没有章纲"
          desc="先写好左边的阶段事件，再让 AI 帮你拆。拆出来之后你可以改、可以拖、可以删。"
          icon="sparkle"
        >
          <AppButton variant="primary" icon="sparkle" @click="generate">AI 拆本章纲</AppButton>
        </AppEmpty>

        <TransitionGroup v-else name="list" tag="ul" class="outline-list">
          <li
            v-for="(o, i) in outlines"
            :key="o.id"
            class="outline-item"
            :class="{
              dragging: dragIndex === i,
              'is-over': overIndex === i && dragIndex !== i,
              confirmed: o.status === 'CONFIRMED'
            }"
            :style="{ animationDelay: `${Math.min(i, 10) * 45}ms` }"
            draggable="true"
            @dragstart="onDragStart(i, $event)"
            @dragover="onDragOver(i, $event)"
            @drop="onDrop(i)"
            @dragend="resetDrag"
          >
            <span class="handle" title="拖动排序"><AppIcon name="drag" :size="15" /></span>

            <span class="cno mono">{{ o.chapterNo }}</span>

            <div class="body">
              <div class="title-row">
                <h4>{{ o.title.replace(/^\d+\.\s*/, '') }}</h4>
                <AppTag
                  :label="o.status === 'CONFIRMED' ? '已确认' : '待确认'"
                  :tone="o.status === 'CONFIRMED' ? 'green' : 'amber'"
                  size="xs"
                  dot
                />
                <AppTag :label="`${o.estimatedWords} 字`" tone="blue" size="xs" />
                <span v-if="o.beats.length < 3 || o.beats.length > 7" class="warn">
                  <AppIcon name="warning" :size="12" /> beat {{ o.beats.length }} 条（需 3—7）
                </span>
              </div>

              <ol class="beats">
                <li v-for="(b, bi) in o.beats" :key="bi">{{ b }}</li>
              </ol>

              <div class="chars">
                <AppIcon name="users" :size="12" />
                <span>{{ o.characters.join('、') || '未指定出场角色' }}</span>
              </div>
            </div>

            <div class="ops">
              <button title="编辑" @click="openEdit(o)"><AppIcon name="edit" :size="14" /></button>
              <button title="在下方插入" @click="insertAfter(o)"><AppIcon name="plus" :size="14" /></button>
              <button title="删除" class="danger" @click="removeOutline(o)">
                <AppIcon name="trash" :size="14" />
              </button>
            </div>
          </li>
        </TransitionGroup>

        <div v-if="outlines.length" class="foot-bar">
          <AppButton variant="ghost" icon="arrowRight" @click="goWriter">确认完成后去创作第一章</AppButton>
        </div>
      </AppCard>
    </div>

    <!-- 编辑章纲 -->
    <AppModal v-model="editOpen" title="编辑章纲" desc="beat 数量必须在 3—7 条之间" icon="edit" :width="560">
      <div v-if="editing" class="ef">
        <AppInput v-model="editForm.title" label="章标题" placeholder="例：破壳" :maxlength="30" />
        <div class="fld">
          <label>Beats（一行一条，{{ editForm.beatsText.split('\n').filter((x) => x.trim()).length }} 条）</label>
          <AppTextarea v-model="editForm.beatsText" :rows="7" placeholder="开场场景与情绪铺垫&#10;本段核心冲突出现&#10;一次尝试失败&#10;章末钩子" />
        </div>
        <AppInput v-model="editForm.characters" label="出场角色" placeholder="用顿号分隔，只能用设定库里已有的角色" />
        <AppInput v-model="wordsText" label="预计字数" type="number" />
      </div>
      <template #footer>
        <AppButton variant="ghost" @click="editOpen = false">取消</AppButton>
        <AppButton variant="primary" icon="check" @click="submitEdit">保存</AppButton>
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

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: var(--text-3);
  animation: fade-in 300ms var(--ease-out) both;
}
.crumb button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--brand-strong);
  font-weight: 600;
}
.crumb button:hover {
  gap: 7px;
}
.crumb b {
  color: var(--text-1);
}

.cols {
  display: grid;
  grid-template-columns: minmax(300px, 380px) 1fr;
  gap: 16px;
  align-items: start;
}

@media (max-width: 1080px) {
  .cols {
    grid-template-columns: 1fr;
  }
}

.events {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 4px 0;
}
.events li {
  position: relative;
  display: flex;
  gap: 10px;
  padding: 8px 4px 8px 0;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
}
.events li::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 30px;
  bottom: -2px;
  width: 1.5px;
  background: linear-gradient(180deg, var(--line-2), transparent);
}
.events li:last-child::before {
  display: none;
}
.dot {
  width: 22px;
  height: 22px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--grad-ocean);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  box-shadow: 0 3px 8px -2px rgba(56, 189, 248, 0.6);
}
.tx {
  font-size: 12.8px;
  color: var(--text-2);
  line-height: 1.65;
  padding-top: 1px;
}

.rh {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}
.rh h3 {
  font-size: 14.5px;
  font-weight: 700;
}
.rh p {
  font-size: 12px;
  color: var(--text-3);
}
.rstat {
  display: flex;
  align-items: center;
  gap: 9px;
}
.rt b {
  font-size: 14px;
  color: var(--text-1);
}
.rt small {
  display: block;
  font-size: 11px;
  color: var(--text-3);
}

.outline-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: relative;
}

.outline-item {
  position: relative;
  display: grid;
  grid-template-columns: 14px 26px 1fr auto;
  gap: 10px;
  align-items: start;
  padding: 13px 14px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: var(--surface-2);
  cursor: grab;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
  transition: transform var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out),
    background var(--dur-base) linear, border-color var(--dur-base) var(--ease-out);
}
.outline-item:hover {
  background: var(--surface);
  border-color: var(--line-2);
  box-shadow: var(--sh-md);
  transform: translateY(-2px);
}
.outline-item.dragging {
  opacity: 0.4;
  transform: scale(0.98);
}
.outline-item.is-over {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
}
.outline-item.confirmed {
  background: linear-gradient(100deg, rgba(52, 211, 153, 0.07), transparent 40%);
  border-color: rgba(52, 211, 153, 0.3);
}

.handle {
  color: var(--text-4);
  padding-top: 3px;
  transition: color var(--dur-fast) var(--ease-out);
}
.outline-item:hover .handle {
  color: var(--brand);
}

.cno {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-strong);
  background: rgba(99, 102, 241, 0.1);
}

.body {
  min-width: 0;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}
.title-row h4 {
  font-size: 14px;
  font-weight: 700;
}
.warn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: var(--c-warn);
  font-weight: 600;
}

.beats {
  margin: 7px 0 6px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.beats li {
  position: relative;
  padding-left: 15px;
  font-size: 12.5px;
  color: var(--text-2);
  line-height: 1.6;
}
.beats li::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 9px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--purple);
}

.chars {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  color: var(--text-3);
}

.ops {
  display: flex;
  gap: 2px;
  flex: none;
}
.ops button {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--text-3);
  opacity: 0;
  transition: all var(--dur-fast) var(--ease-out);
}
.outline-item:hover .ops button {
  opacity: 1;
}
.ops button:hover {
  background: rgba(99, 102, 241, 0.1);
  color: var(--brand-strong);
}
.ops button.danger:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--c-danger);
}

.foot-bar {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--line-2);
  display: flex;
  justify-content: flex-end;
}

.ef {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.fld label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  margin-bottom: 6px;
}
</style>
