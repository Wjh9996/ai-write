<script setup lang="ts">
/** 大纲页：总纲 + 卷纲事件 + 拆章纲入口 */
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { useUiStore } from '@/stores/ui'
import { outlineApi } from '@/api'
import type { Volume } from '@/types'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import AppTextarea from '@/components/common/AppTextarea.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AppSkeleton from '@/components/common/AppSkeleton.vue'

const novel = useNovelStore()
const ui = useUiStore()
const router = useRouter()

const outlineText = ref('')
const dirty = ref(false)
const saving = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const creating = ref(false)
const newVolumeTitle = ref('')
const pendingVolume = ref<Volume | null>(null)
const generating = ref(false)

const VS: Record<string, { label: string; tone: 'gray' | 'amber' | 'purple' | 'green' }> = {
  RAW: { label: '待写事件', tone: 'gray' },
  OUTLINED: { label: '事件已就绪', tone: 'amber' },
  PENDING_CONFIRM: { label: '章纲待确认', tone: 'purple' },
  CONFIRMED: { label: '已确认', tone: 'green' }
}

const outlineWords = computed(() => (outlineText.value.match(/[\u4e00-\u9fa5]/g) ?? []).length)

function syncFromStore() {
  outlineText.value = novel.current?.mainOutline ?? ''
  dirty.value = false
}

onMounted(syncFromStore)
watch(() => novel.current?.id, syncFromStore)

async function saveOutline() {
  saving.value = true
  try {
    await novel.saveMainOutline(outlineText.value)
    dirty.value = false
    ui.success('总纲已保存', `${outlineWords.value} 字`)
  } catch (e) {
    ui.error('保存失败', (e as Error).message)
  } finally {
    saving.value = false
  }
}

function onEdit() {
  dirty.value = true
}

function triggerImport() {
  fileInput.value?.click()
}

async function onFile(ev: Event) {
  const el = ev.target as HTMLInputElement
  const file = el.files?.[0]
  el.value = ''
  if (!file) return
  ui.showLoading('正在识别编码并解析…')
  try {
    await novel.importTxt(file)
    syncFromStore()
    ui.success('导入完成', '已覆盖当前总纲，并按「第X卷」拆分了卷列表')
  } catch {
    ui.error('导入失败', '请使用 UTF-8 编码的 TXT 文件')
  } finally {
    ui.hideLoading()
  }
}

async function createVolume() {
  if (!newVolumeTitle.value.trim()) return
  const v = await outlineApi.createVolume(novel.currentId, newVolumeTitle.value.trim())
  await novel.loadVolumes()
  creating.value = false
  newVolumeTitle.value = ''
  ui.success('已新增卷', v.title)
}

async function generateOutlines(v: Volume) {
  pendingVolume.value = v
  generating.value = true
  ui.showLoading('AI 正在按本卷事件拆章纲…')
  try {
    const res = await outlineApi.generate(v.id)
    await novel.loadVolumes()
    ui.success('章纲已生成', `${res.outlines.length} 条待确认，另提出 ${res.proposedEntities.length} 个新设定`)
    router.push(`/novel/${novel.currentId}/outline/volume/${v.id}`)
  } catch (e) {
    ui.error('拆章纲失败', (e as Error).message)
  } finally {
    ui.hideLoading()
    generating.value = false
    pendingVolume.value = null
  }
}
</script>

<template>
  <div class="page">
    <!-- 总纲 -->
    <AppCard
      title="全书总纲"
      desc="主线 + 各卷大致走向。它是所有上下文的源头，改它会重新进入「设定初稿」流程"
      icon="outline"
      tone="brand"
    >
      <template #actions>
        <AppButton size="sm" variant="outline" icon="upload" @click="triggerImport">导入 TXT</AppButton>
        <AppButton
          size="sm"
          :variant="dirty ? 'primary' : 'soft'"
          icon="check"
          :loading="saving"
          @click="saveOutline"
        >
          {{ dirty ? '保存修改' : '已保存' }}
        </AppButton>
      </template>

      <AppTextarea
        v-if="novel.current"
        v-model="outlineText"
        :rows="12"
        :max-height="360"
        show-count
        placeholder="例：开局在废矿捡到一枚灰蛋 → 孵化出第一只兽宠 → 参加宝宝赛 → 晋升高级学府 → …（越具体，AI 拆出来的章纲越贴合）"
        @input="onEdit"
      />
      <AppSkeleton v-else :lines="6" />

      <div class="hint">
        <AppIcon name="info" :size="14" />
        <span>
          中文 {{ outlineWords }} 字 ·
          {{ dirty ? '<strong>有未保存的修改</strong>' : '与服务端一致' }}
          —— 建议先写主线，再按阶段补事件
        </span>
      </div>

      <input ref="fileInput" type="file" accept=".txt,text/plain" hidden @change="onFile" />
    </AppCard>

    <!-- 卷列表 -->
    <AppCard title="卷（阶段）" desc="一个卷 = 一个完整阶段，约 15—40 章" icon="layers" tone="violet">
      <template #actions>
        <AppButton size="sm" variant="primary" icon="plus" @click="creating = true">新增卷</AppButton>
      </template>

      <div v-if="!novel.volumes.length" class="mt">
        <AppEmpty
          title="还没有卷"
          desc="把主线拆成几个阶段，每个阶段写 5—10 条事件，AI 就能照着拆出章纲"
          icon="layers"
        >
          <AppButton variant="primary" icon="plus" @click="creating = true">新增第一卷</AppButton>
        </AppEmpty>
      </div>

      <TransitionGroup v-else name="list" tag="ul" class="vol-list">
        <li
          v-for="(v, i) in novel.volumes"
          :key="v.id"
          class="vol-item"
          :style="{ animationDelay: `${i * 60}ms` }"
        >
          <span class="no" :style="{ background: `linear-gradient(135deg,hsl(${240 + i * 26} 85% 66%),hsl(${290 + i * 22} 85% 66%))` }">
            {{ v.volumeNo }}
          </span>

          <div class="meta">
            <h4>{{ v.title }}</h4>
            <div class="tags">
              <AppTag :label="VS[v.status]?.label" :tone="VS[v.status]?.tone" dot />
              <AppTag :label="`${v.events.length} 条事件`" tone="gray" icon="target" />
              <AppTag :label="`预计 ${v.targetChapters ?? 18} 章`" tone="blue" icon="file" />
            </div>
            <p v-if="v.events.length" class="preview">
              {{ v.events.map((e) => e.content).join(' → ') }}
            </p>
            <p v-else class="preview muted">还没写阶段事件，点右侧「编辑事件」补上</p>
          </div>

          <div class="ops">
            <AppButton
              size="sm"
              variant="soft"
              icon="edit"
              @click="router.push(`/novel/${novel.currentId}/outline/volume/${v.id}`)"
            >
              查看/编辑
            </AppButton>
            <AppButton
              size="sm"
              :variant="v.status === 'CONFIRMED' ? 'outline' : 'primary'"
              icon="sparkle"
              :loading="generating && pendingVolume?.id === v.id"
              @click="generateOutlines(v)"
            >
              {{ v.status === 'CONFIRMED' ? '重新拆分' : 'AI 拆章纲' }}
            </AppButton>
          </div>

          <i class="hover-bar" aria-hidden="true" />
        </li>
      </TransitionGroup>
    </AppCard>

    <AppModal v-model="creating" title="新增卷" desc="一卷对应主线里的一个阶段" icon="plus" :width="440">
      <AppInput v-model="newVolumeTitle" label="卷标题" placeholder="例如：第二卷 · 宝宝赛" :maxlength="30" />
      <template #footer>
        <AppButton variant="ghost" @click="creating = false">取消</AppButton>
        <AppButton variant="primary" icon="check" @click="createVolume">新增</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.page {
  padding: 22px 24px 60px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1180px;
}

.hint {
  display: flex;
  gap: 7px;
  align-items: center;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-3);
  background: var(--surface-3);
  border-radius: var(--r-sm);
  padding: 8px 11px;
}
.hint :deep(strong) {
  color: var(--c-warn);
}

.mt {
  margin-top: 6px;
}

.vol-list {
  display: flex;
  flex-direction: column;
  gap: 11px;
  position: relative;
}

.vol-item {
  position: relative;
  display: grid;
  grid-template-columns: 46px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 14px 15px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: var(--surface-2);
  overflow: hidden;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
  transition: transform var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out),
    border-color var(--dur-base) var(--ease-out);
}
.vol-item:hover {
  transform: translateX(3px);
  border-color: var(--line-2);
  box-shadow: var(--sh-md);
  background: var(--surface);
}

.no {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 18px;
  box-shadow: var(--sh-sm);
  transition: transform var(--dur-base) var(--ease-spring);
}
.vol-item:hover .no {
  transform: scale(1.08) rotate(-6deg);
}

.meta {
  min-width: 0;
}
.meta h4 {
  font-size: 14.5px;
  font-weight: 700;
}
.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin: 6px 0 5px;
}
.preview {
  font-size: 12px;
  color: var(--text-3);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.6;
}
.preview.muted {
  color: var(--text-4);
  font-style: italic;
}

.ops {
  display: flex;
  gap: 8px;
  flex: none;
}

.hover-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--grad-brand);
  transform: scaleY(0);
  transform-origin: top;
  transition: transform var(--dur-base) var(--ease-out);
}
.vol-item:hover .hover-bar {
  transform: scaleY(1);
}
</style>
