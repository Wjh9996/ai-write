<script setup lang="ts">
/** 设定库：角色 / 世界观 / 时间线 / 伏笔 / 文风 */
import { computed, ref, watch } from 'vue'
import { useNovelStore } from '@/stores/novel'
import { useUiStore } from '@/stores/ui'
import { settingsApi } from '@/api'
import type { SettingEntity, WorldSetting } from '@/types'
import AppCard from '@/components/common/AppCard.vue'
import AppTabs from '@/components/common/AppTabs.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppTextarea from '@/components/common/AppTextarea.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const novel = useNovelStore()
const ui = useUiStore()

const tab = ref('character')
const TABS = [
  { key: 'character', label: '角色' },
  { key: 'place', label: '地点/势力/物品' },
  { key: 'world', label: '世界观' },
  { key: 'timeline', label: '时间线' },
  { key: 'plant', label: '伏笔' },
  { key: 'style', label: '文风卡' }
]

const TYPE_LABEL: Record<string, string> = {
  CHARACTER: '角色',
  PLACE: '地点',
  ORGANIZATION: '势力',
  ITEM: '物品'
}

const entities = computed(() => novel.settings?.entities ?? [])
const characters = computed(() => entities.value.filter((e) => e.type === 'CHARACTER'))
const others = computed(() => entities.value.filter((e) => e.type !== 'CHARACTER'))
const worlds = computed(() => novel.settings?.worlds ?? [])
const timeline = computed(() => novel.settings?.timeline ?? [])
const plants = computed(() => novel.settings?.foreshadowings ?? [])
const style = computed(() => novel.settings?.style)

// ---- AI 抽取 ----
const extracting = ref(false)
async function extract() {
  extracting.value = true
  try {
    const res = await settingsApi.extract(novel.currentId)
    const s = novel.settings
    if (s) {
      s.entities.push(...res.entities)
      s.worlds.push(...res.worlds)
    }
    ui.success(`AI 抽取出 ${res.entities.length} 个设定 + ${res.worlds.length} 条世界规则`, '已标为 AI 初稿，需要你确认')
  } catch (e) {
    ui.error('抽取失败', (e as Error).message)
  } finally {
    extracting.value = false
  }
}

// ---- 实体编辑 ----
const entityOpen = ref(false)
const editingEntity = ref<SettingEntity | null>(null)
const cardText = ref('')
const aliasText = ref('')
const revealText = ref('')

function isNew(e: SettingEntity | null): boolean {
  return !e
}

function openEntity(e?: SettingEntity) {
  editingEntity.value = e
    ? { ...e }
    : {
        id: 0,
        novelId: novel.currentId,
        type: tab.value === 'place' ? 'PLACE' : 'CHARACTER',
        name: '',
        aliases: [],
        card: {},
        source: 'MANUAL',
        approvalStatus: 'DRAFT',
        revealChapter: null,
        updatedAt: ''
      }
  cardText.value = Object.entries(e?.card ?? {})
    .map(([k, v]) => `${k}：${v}`)
    .join('\n')
  aliasText.value = (e?.aliases ?? []).join('，')
  revealText.value = e?.revealChapter ? String(e.revealChapter) : ''
  entityOpen.value = true
}

async function saveEntity() {
  const e = editingEntity.value
  if (!e) return
  if (!e.name.trim()) {
    ui.warn('名字不能为空')
    return
  }
  e.aliases = aliasText.value
    .split(/[,，、]/)
    .map((a) => a.trim())
    .filter(Boolean)
  e.revealChapter = revealText.value.trim() ? Number(revealText.value) : null
  e.card = Object.fromEntries(
    cardText.value
      .split('\n')
      .map((l) => l.trim())
      .filter(Boolean)
      .map((l) => {
        const idx = l.search(/[：:]/)
        if (idx < 0) return ['备注', l] as [string, string]
        return [l.slice(0, idx).trim(), l.slice(idx + 1).trim()] as [string, string]
      })
  )
  const saved = await settingsApi.saveEntity(novel.currentId, e)
  const s = novel.settings
  if (s) {
    const idx = s.entities.findIndex((x) => x.id === saved.id)
    if (idx >= 0) s.entities[idx] = saved
    else s.entities.push(saved)
  }
  entityOpen.value = false
  ui.success(isNew(editingEntity.value) ? '设定已新增' : '设定已保存', e.name)
}

async function removeEntity(id: number) {
  await settingsApi.removeEntity(id)
  const s = novel.settings
  if (s) s.entities = s.entities.filter((e) => e.id !== id)
  ui.info('已删除')
}

// ---- 世界观编辑 ----
const worldOpen = ref(false)
const editingWorld = ref<WorldSetting | null>(null)
const worldForm = ref({ category: '世界规则', title: '', content: '' })

function openWorld(w?: WorldSetting) {
  editingWorld.value = w ?? null
  worldForm.value = w
    ? { category: w.category, title: w.title, content: w.content }
    : { category: '世界规则', title: '', content: '' }
  worldOpen.value = true
}

function saveWorld() {
  const s = novel.settings
  if (!s) return
  if (!worldForm.value.title.trim()) {
    ui.warn('标题不能为空')
    return
  }
  const existing = editingWorld.value
  if (existing) {
    const idx = s.worlds.findIndex((x) => x.id === existing.id)
    if (idx >= 0) {
      s.worlds[idx] = { ...s.worlds[idx], ...worldForm.value }
    }
  } else {
    s.worlds.push({
      id: Date.now(),
      novelId: novel.currentId,
      source: 'MANUAL',
      approvalStatus: 'CONFIRMED',
      ...worldForm.value
    })
  }
  settingsApi.save(novel.currentId, s)
  worldOpen.value = false
  ui.success('世界观已保存')
}

// ---- 文风卡 ----
const styleText = ref('')
watch(
  style,
  (s) => {
    styleText.value = s
      ? Object.entries(s.card)
          .map(([k, v]) => `${k}：${v}`)
          .join('\n')
      : ''
  },
  { immediate: true }
)

async function saveStyle() {
  const s = novel.settings?.style
  if (!s) return
  s.card = Object.fromEntries(
    styleText.value
      .split('\n')
      .map((l) => l.trim())
      .filter(Boolean)
      .map((l) => {
        const idx = l.search(/[：:]/)
        if (idx < 0) return ['备注', l] as [string, string]
        return [l.slice(0, idx).trim(), l.slice(idx + 1).trim()] as [string, string]
      })
  )
  const saved = await settingsApi.saveStyle(novel.currentId, s)
  novel.setStyle(saved)
  ui.success('文风卡已保存', '后续所有生成都会带上它')
}

const styleCards = computed(() =>
  Object.entries(style.value?.card ?? {}).map(([k, v]) => ({ k, v }))
)
</script>

<template>
  <div class="page">
    <header class="hd">
      <div>
        <h2>设定库</h2>
        <p>只有「已确认」的设定才会进入 AI 上下文。AI 初稿需要你过一遍。</p>
      </div>
      <AppButton variant="primary" icon="sparkle" :loading="extracting" @click="extract">
        AI 从总纲抽取初稿
      </AppButton>
    </header>

    <AppTabs v-model="tab" :items="TABS" size="md" />

    <!-- 角色 -->
    <section v-if="tab === 'character' || tab === 'place'" class="grid">
      <AppCard v-for="e in (tab === 'character' ? characters : others)" :key="e.id" hoverable :padding="'md'">
        <template #header>
          <div class="eh">
            <div class="eh-left">
              <span class="avatar" :class="`ty-${e.type.toLowerCase()}`">{{ e.name.slice(0, 1) }}</span>
              <div>
                <h3>
                  {{ e.name }}
                  <small v-if="e.aliases.length">{{ e.aliases.join(' / ') }}</small>
                </h3>
                <div class="tags">
                  <AppTag :label="TYPE_LABEL[e.type]" tone="brand" size="xs" />
                  <AppTag
                    :label="e.source === 'AI' ? 'AI 初稿' : '人工'"
                    :tone="e.source === 'AI' ? 'purple' : 'teal'"
                    size="xs"
                  />
                  <AppTag
                    :label="e.approvalStatus === 'CONFIRMED' ? '已确认' : '待确认'"
                    :tone="e.approvalStatus === 'CONFIRMED' ? 'green' : 'amber'"
                    size="xs"
                    dot
                  />
                  <AppTag
                    v-if="e.revealChapter"
                    :label="`首次揭露：第 ${e.revealChapter} 章`"
                    tone="rose"
                    size="xs"
                    icon="shield"
                  />
                </div>
              </div>
            </div>
          </div>
        </template>
        <template #actions>
          <button class="mn" title="编辑" @click="openEntity(e)"><AppIcon name="edit" :size="15" /></button>
          <button class="mn danger" title="删除" @click="removeEntity(e.id)">
            <AppIcon name="trash" :size="15" />
          </button>
        </template>

        <dl class="kv">
          <div v-for="(v, k) in e.card" :key="k">
            <dt>{{ k }}</dt>
            <dd>{{ v }}</dd>
          </div>
        </dl>
        <p v-if="!Object.keys(e.card).length" class="muted">这张卡还是空的，点右上角补一下</p>
      </AppCard>

      <button class="add" @click="openEntity()">
        <AppIcon name="plus" :size="22" />
        <span>新增{{ tab === 'character' ? '角色' : '地点 / 势力 / 物品' }}</span>
        <small>也可以用左上角的按钮让 AI 从总纲里抽</small>
      </button>
    </section>

    <!-- 世界观 -->
    <section v-else-if="tab === 'world'" class="grid">
      <AppCard v-for="w in worlds" :key="w.id" hoverable :tone="w.approvalStatus === 'CONFIRMED' ? 'none' : 'violet'">
        <template #header>
          <div class="eh-left">
            <span class="avatar ty-world"><AppIcon name="globe" :size="15" color="#fff" /></span>
            <div>
              <h3>{{ w.title }}</h3>
              <div class="tags">
                <AppTag :label="w.category" tone="ocean" size="xs" />
                <AppTag :label="w.source === 'AI' ? 'AI 初稿' : '人工'" :tone="w.source === 'AI' ? 'purple' : 'teal'" size="xs" />
              </div>
            </div>
          </div>
        </template>
        <template #actions>
          <button class="mn" @click="openWorld(w)"><AppIcon name="edit" :size="15" /></button>
        </template>
        <p class="wt">{{ w.content }}</p>
      </AppCard>

      <button class="add" @click="openWorld()">
        <AppIcon name="plus" :size="22" />
        <span>新增世界规则</span>
        <small>契约规则、境界划分、地理、势力……</small>
      </button>
    </section>

    <!-- 时间线 -->
    <section v-else-if="tab === 'timeline'" class="timeline">
      <div v-for="(t, i) in timeline" :key="t.id" class="tl-item" :style="{ animationDelay: `${i * 70}ms` }">
        <span class="axis"><i /></span>
        <div class="tl-body">
          <span class="time mono">{{ t.timeLabel }}</span>
          <h4>{{ t.event }}</h4>
          <AppTag
            :label="t.approvalStatus === 'CONFIRMED' ? '已确认' : '草稿'"
            :tone="t.approvalStatus === 'CONFIRMED' ? 'green' : 'amber'"
            size="xs"
            dot
          />
        </div>
      </div>
      <AppEmpty v-if="!timeline.length" title="时间线上还没有事件" desc="章节审核入库后，AI 提议的时间线变更会进到这里" icon="clock" />
    </section>

    <!-- 伏笔 -->
    <section v-else-if="tab === 'plant'" class="grid">
      <AppCard v-for="f in plants" :key="f.id" hoverable :tone="f.status === 'PAID_OFF' ? 'mint' : 'sunset'">
        <template #actions>
          <AppTag :label="f.status === 'PAID_OFF' ? '已回收' : '已埋未收'" :tone="f.status === 'PAID_OFF' ? 'green' : 'amber'" size="xs" dot />
        </template>
        <p class="wt">{{ f.content }}</p>
        <div class="pt-row">
          <span><AppIcon name="target" :size="12" /> 埋于第 {{ f.setChapterId }} 章</span>
          <span v-if="f.payoffChapterId">回收于第 {{ f.payoffChapterId }} 章</span>
          <span v-else class="warn"><AppIcon name="warning" :size="12" /> 还没安排回收章</span>
        </div>
      </AppCard>
      <AppEmpty v-if="!plants.length" title="还没有伏笔" icon="link" />
    </section>

    <!-- 文风卡 -->
    <section v-else class="style-wrap">
      <AppCard title="文风卡" desc="所有章节生成都会带上这张卡，改它会立刻影响后续正文" icon="palette" tone="pink">
        <template #actions>
          <AppButton size="sm" variant="primary" icon="check" @click="saveStyle">保存文风</AppButton>
        </template>
        <div class="style-grid">
          <div class="left">
            <label>风格项（一行一条，格式「名：内容」）</label>
            <AppTextarea v-model="styleText" :rows="12" />
          </div>
          <div class="right">
            <div class="chips">
              <span v-for="c in styleCards" :key="c.k" class="chip">
                <b>{{ c.k }}</b>{{ c.v }}
              </span>
            </div>
            <div class="sample">
              <label>样章（贴一段你最满意的手写片段）</label>
              <AppTextarea
                v-if="style"
                v-model="style.sampleText"
                :rows="8"
                placeholder="贴进来，AI 会学着它的节奏写"
              />
            </div>
            <div class="forbid">
              <label>禁用词 / 禁止写法</label>
              <div class="fb">
                <span v-for="t in style?.forbiddenTerms ?? []" :key="t">{{ t }}</span>
              </div>
            </div>
          </div>
        </div>
      </AppCard>
    </section>

    <!-- 实体弹窗 -->
    <AppModal v-model="entityOpen" title="编辑设定卡" desc="「key：value」一行一条；标为已确认后才会进上下文" icon="user" :width="560">
      <div v-if="editingEntity" class="ef">
        <div class="row2">
          <AppInput v-model="editingEntity.name" label="名称" placeholder="例：灰羽" :maxlength="20" />
          <div class="sel">
            <label>类型</label>
            <select v-model="editingEntity.type">
              <option v-for="(l, k) in TYPE_LABEL" :key="k" :value="k">{{ l }}</option>
            </select>
          </div>
        </div>
        <AppInput v-model="aliasText" label="别名（逗号分隔）" placeholder="小东西,灰团子" />
        <div class="fld">
          <label>设定内容</label>
          <AppTextarea v-model="cardText" :rows="7" placeholder="身份：林昭的第一只契约兽&#10;阶段：幼生期&#10;能力：拟态 · 一阶段" />
        </div>
        <div class="row2">
          <AppInput v-model="revealText" label="首次揭露章号" placeholder="留空=随时可见" />
          <div class="sel">
            <label>确认状态</label>
            <select v-model="editingEntity.approvalStatus">
              <option value="DRAFT">草稿（不进上下文）</option>
              <option value="CONFIRMED">已确认（进上下文）</option>
              <option value="REJECTED">已驳回</option>
            </select>
          </div>
        </div>
      </div>
      <template #footer>
        <AppButton variant="ghost" @click="entityOpen = false">取消</AppButton>
        <AppButton variant="primary" icon="check" @click="saveEntity">保存</AppButton>
      </template>
    </AppModal>

    <!-- 世界观弹窗 -->
    <AppModal v-model="worldOpen" title="世界设定" icon="globe" :width="520">
      <div class="ef">
        <div class="row2">
          <div class="sel">
            <label>分类</label>
            <select v-model="worldForm.category">
              <option>世界规则</option>
              <option>地理</option>
              <option>势力</option>
              <option>历史</option>
            </select>
          </div>
          <AppInput v-model="worldForm.title" label="标题" placeholder="例：契约规则" />
        </div>
        <div class="fld">
          <label>内容</label>
          <AppTextarea v-model="worldForm.content" :rows="6" />
        </div>
      </div>
      <template #footer>
        <AppButton variant="ghost" @click="worldOpen = false">取消</AppButton>
        <AppButton variant="primary" icon="check" @click="saveWorld">保存</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.page {
  padding: 22px 24px 60px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1240px;
}

.hd {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  animation: fade-up 420ms var(--ease-out) both;
}
.hd h2 {
  font-size: 20px;
  font-weight: 800;
}
.hd p {
  font-size: 12.5px;
  color: var(--text-3);
  margin-top: 2px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 14px;
}

.eh-left {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 800;
  font-size: 16px;
  flex: none;
  background: var(--grad-brand);
  box-shadow: var(--sh-brand);
}
.ty-place,
.ty-item {
  background: var(--grad-sunset);
}
.ty-organization {
  background: var(--grad-mint);
}
.ty-world {
  background: var(--grad-ocean);
}

.eh h3 {
  font-size: 14.5px;
  font-weight: 700;
}
.eh h3 small {
  font-size: 11.5px;
  color: var(--text-4);
  font-weight: 500;
  margin-left: 5px;
}
.tags {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
  margin-top: 5px;
}

.mn {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: var(--text-3);
  transition: all var(--dur-fast) var(--ease-out);
}
.mn:hover {
  background: rgba(99, 102, 241, 0.1);
  color: var(--brand-strong);
}
.mn.danger:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--c-danger);
}

.kv {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kv > div {
  display: grid;
  grid-template-columns: 74px 1fr;
  gap: 8px;
  align-items: start;
  font-size: 12.5px;
}
.kv dt {
  color: var(--text-3);
  font-weight: 600;
}
.kv dd {
  color: var(--text-1);
  line-height: 1.6;
  margin: 0;
}
.muted {
  font-size: 12.5px;
  color: var(--text-4);
}

.add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 180px;
  border: 1.6px dashed var(--line-strong);
  border-radius: var(--r-lg);
  color: var(--text-3);
  background: var(--surface-2);
  transition: all var(--dur-base) var(--ease-out);
}
.add:hover {
  border-color: var(--brand);
  color: var(--brand-strong);
  background: rgba(99, 102, 241, 0.04);
  transform: translateY(-3px);
}
.add span {
  font-size: 13.5px;
  font-weight: 600;
}
.add small {
  font-size: 11.5px;
  color: var(--text-4);
}

.wt {
  font-size: 12.8px;
  color: var(--text-2);
  line-height: 1.75;
}

.pt-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 10px;
  font-size: 11.5px;
  color: var(--text-3);
}
.pt-row span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.pt-row .warn {
  color: var(--c-warn);
  font-weight: 600;
}

/* 时间线 */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 4px;
}
.tl-item {
  display: grid;
  grid-template-columns: 26px 1fr;
  gap: 12px;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
}
.axis {
  position: relative;
  display: grid;
  justify-items: center;
}
.axis::before {
  content: '';
  position: absolute;
  top: 18px;
  bottom: -14px;
  width: 2px;
  background: linear-gradient(180deg, var(--purple), rgba(168, 85, 247, 0.15));
  border-radius: 2px;
}
.axis i {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: var(--grad-violet);
  box-shadow: 0 0 0 3px rgba(168, 85, 247, 0.18);
  margin-top: 4px;
  z-index: 1;
}
.tl-body {
  padding-bottom: 16px;
}
.time {
  font-size: 11.5px;
  color: var(--purple);
  font-weight: 700;
}
.tl-body h4 {
  font-size: 13.5px;
  font-weight: 600;
  margin: 2px 0 5px;
}

/* 文风 */
.style-wrap {
  max-width: 1000px;
}
.style-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.style-grid label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  margin-bottom: 6px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 14px;
}
.chip {
  font-size: 11.5px;
  color: var(--text-2);
  background: rgba(99, 102, 241, 0.07);
  border: 1px solid rgba(99, 102, 241, 0.16);
  border-radius: var(--r-sm);
  padding: 5px 9px;
  animation: pop-in var(--dur-base) var(--ease-spring) both;
}
.chip b {
  color: var(--brand-strong);
  margin-right: 5px;
  font-weight: 700;
}
.fb {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.fb span {
  font-size: 11.5px;
  padding: 3px 9px;
  border-radius: var(--r-full);
  background: rgba(239, 68, 68, 0.08);
  color: var(--c-danger);
  border: 1px solid rgba(239, 68, 68, 0.18);
  font-weight: 600;
}

/* 弹窗表单 */
.ef {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.row2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.sel label,
.fld label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  margin-bottom: 6px;
}
.sel select {
  width: 100%;
  height: 38px;
  border-radius: var(--r-md);
  border: 1.5px solid var(--line-2);
  padding: 0 10px;
  background: var(--surface);
  font-size: 13px;
  outline: none;
  transition: border-color var(--dur-base) var(--ease-out);
}
.sel select:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.14);
}

@media (max-width: 900px) {
  .style-grid {
    grid-template-columns: 1fr;
  }
}
</style>
