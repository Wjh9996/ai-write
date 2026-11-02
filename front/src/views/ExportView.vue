<script setup lang="ts">
/** 导出页：范围/格式/是否保留标题 */
import { computed, onMounted, ref } from 'vue'
import { useNovelStore } from '@/stores/novel'
import { useUiStore } from '@/stores/ui'
import { exportApi } from '@/api'
import type { ExportFormat, ExportScope } from '@/types'
import AppCard from '@/components/common/AppCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import AppSwitch from '@/components/common/AppSwitch.vue'
import AppProgress from '@/components/common/AppProgress.vue'

const novel = useNovelStore()
const ui = useUiStore()

const scope = ref<ExportScope>('BOOK')
const format = ref<ExportFormat>('docx')
const withTitle = ref(true)
const withAuthor = ref(false)
const exporting = ref(false)
const progress = ref(0)
const result = ref<{ fileName: string; sizeKb: number } | null>(null)
const histories = ref<{ id: number; fileName: string; scope: string; createdAt: string; sizeKb: number }[]>([])

const SCOPES: { key: ExportScope; title: string; desc: string; icon: string }[] = [
  { key: 'CHAPTER', title: '单章', desc: '导出某一章，用于发布或投稿', icon: 'file' },
  { key: 'VOLUME', title: '单卷', desc: '保留卷标题与章标题层级', icon: 'layers' },
  { key: 'BOOK', title: '全书', desc: '全部卷 + 章节，按章号排序', icon: 'book' }
]

const FORMATS: { key: ExportFormat; title: string; desc: string }[] = [
  { key: 'docx', title: 'Word (.docx)', desc: '保留标题层级，可直接投稿' },
  { key: 'md', title: 'Markdown (.md)', desc: '带 # 号标题，便于二次编辑' },
  { key: 'txt', title: '纯文本 (.txt)', desc: '干净正文，适合备份' }
]

const approved = computed(() => novel.chapters.filter((c) => c.chapterStatus === 'APPROVED').length)
const chapterPicker = ref(0)

onMounted(async () => {
  histories.value = await exportApi.history(novel.currentId)
  if (novel.chapters.length) chapterPicker.value = novel.chapters[novel.chapters.length - 1].id
})

async function run() {
  exporting.value = true
  progress.value = 0
  result.value = null
  const timer = setInterval(() => {
    progress.value = Math.min(96, progress.value + 7 + Math.random() * 14)
  }, 180)
  try {
    const res = await exportApi.run(novel.currentId, {
      scope: scope.value,
      format: format.value,
      withTitle: withTitle.value,
      withAuthor: withAuthor.value
    })
    progress.value = 100
    result.value = { fileName: res.fileName, sizeKb: res.sizeKb }
    histories.value = await exportApi.history(novel.currentId)
    ui.success('导出完成', res.fileName)
  } catch (e) {
    ui.error('导出失败', (e as Error).message)
  } finally {
    clearInterval(timer)
    exporting.value = false
  }
}

/** 真正下载：后端 /export 返回二进制流，浏览器直连（token 走 query） */
function download() {
  const url = exportApi.downloadUrl(novel.currentId, {
    scope: scope.value,
    format: format.value,
    withTitle: withTitle.value,
    withAuthor: withAuthor.value
  })
  const a = document.createElement('a')
  a.href = url
  a.download = result.value?.fileName ?? 'export'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

const scopeLabel = computed(() => SCOPES.find((s) => s.key === scope.value)?.title ?? '')
</script>

<template>
  <div class="page">
    <AppCard title="导出作品" desc="导出记录会留痕，导出本身不是不可逆操作 —— 正文永远以章节为准" icon="export" tone="ocean">
      <div class="steps">
        <!-- 1 范围 -->
        <section class="step">
          <h4><span class="n">1</span> 选择范围</h4>
          <div class="opts">
            <button
              v-for="s in SCOPES"
              :key="s.key"
              class="opt"
              :class="{ on: scope === s.key }"
              @click="scope = s.key"
            >
              <span class="oi"><AppIcon :name="s.icon" :size="17" /></span>
              <div>
                <b>{{ s.title }}</b>
                <small>{{ s.desc }}</small>
              </div>
              <i class="tick" v-if="scope === s.key"><AppIcon name="check" :size="12" color="#fff" /></i>
            </button>
          </div>

          <div v-if="scope === 'CHAPTER'" class="picker">
            <label>选择章节</label>
            <select v-model="chapterPicker">
              <option v-for="c in novel.chapters" :key="c.id" :value="c.id">
                第 {{ c.chapterNo }} 章 · {{ c.title.replace(/^\d+\.\s*/, '') }}
              </option>
            </select>
          </div>
        </section>

        <!-- 2 格式 -->
        <section class="step">
          <h4><span class="n">2</span> 选择格式</h4>
          <div class="opts fmts">
            <button
              v-for="f in FORMATS"
              :key="f.key"
              class="opt"
              :class="{ on: format === f.key }"
              @click="format = f.key"
            >
              <span class="oi"><AppIcon name="file" :size="17" /></span>
              <div>
                <b>{{ f.title }}</b>
                <small>{{ f.desc }}</small>
              </div>
              <i class="tick" v-if="format === f.key"><AppIcon name="check" :size="12" color="#fff" /></i>
            </button>
          </div>

          <div class="switches">
            <div class="sw-row">
              <div>
                <b>保留卷 / 章标题</b>
                <small>Word 里体现为层级标题，便于生成目录</small>
              </div>
              <AppSwitch v-model="withTitle" />
            </div>
            <div class="sw-row">
              <div>
                <b>附带作者信息</b>
                <small>封面页追加作者名与创作台来源</small>
              </div>
              <AppSwitch v-model="withAuthor" />
            </div>
          </div>
        </section>

        <!-- 3 执行 -->
        <section class="step">
          <h4><span class="n">3</span> 执行导出</h4>
          <div class="summary">
            <div class="sm-row"><span>范围</span><b>{{ scopeLabel }}</b></div>
            <div class="sm-row"><span>格式</span><b>.{{ format }}</b></div>
            <div class="sm-row"><span>包含章节</span><b>{{ scope === 'BOOK' ? novel.chapters.length : '—' }} 章</b></div>
            <div class="sm-row"><span>其中已审核</span><b>{{ approved }} 章</b></div>
          </div>

          <AppProgress v-if="exporting" :value="progress" :height="8" striped tone="56,189,248" />

          <AppButton variant="primary" size="lg" block icon="download" :loading="exporting" @click="run">
            {{ exporting ? `正在导出 ${Math.round(progress)}%` : '开始导出' }}
          </AppButton>

          <div v-if="result" class="ok anim-pop">
            <AppIcon name="check" :size="16" color="#fff" />
            <div class="grow">
              <b>{{ result.fileName }}</b>
              <small>{{ result.sizeKb }} KB · 点右侧按钮保存到本地</small>
            </div>
            <button class="dl" title="下载文件" @click="download">
              <AppIcon name="download" :size="15" />
            </button>
          </div>

          <p v-if="approved < novel.chapters.length" class="warn">
            <AppIcon name="warning" :size="13" />
            有 {{ novel.chapters.length - approved }} 章还没审核通过，导出会带上未定稿内容
          </p>
        </section>
      </div>
    </AppCard>

    <AppCard title="导出记录" desc="导出是操作留痕，不影响章节状态" icon="clock" tone="none">
      <ul v-if="histories.length" class="hist">
        <li v-for="(h, i) in histories" :key="h.id" :style="{ animationDelay: `${i * 60}ms` }">
          <span class="hi"><AppIcon name="download" :size="15" /></span>
          <div class="hb">
            <b>{{ h.fileName }}</b>
            <small>{{ new Date(h.createdAt).toLocaleString('zh-CN') }} · {{ h.sizeKb }} KB</small>
          </div>
          <AppTag :label="h.scope === 'BOOK' ? '全书' : h.scope === 'VOLUME' ? '单卷' : '单章'" tone="blue" size="xs" />
        </li>
      </ul>
      <p v-else class="muted">还没有导出记录</p>
    </AppCard>
  </div>
</template>

<style scoped>
.page {
  padding: 22px 24px 60px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1120px;
}

.steps {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 18px;
}

.step h4 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-2);
  margin-bottom: 11px;
}
.n {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: var(--grad-brand);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
}

.opts {
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.opt {
  position: relative;
  display: grid;
  grid-template-columns: 36px 1fr;
  gap: 11px;
  align-items: center;
  padding: 11px 12px;
  border-radius: var(--r-md);
  border: 1.5px solid var(--line);
  background: var(--surface-2);
  text-align: left;
  transition: all var(--dur-base) var(--ease-out);
}
.opt:hover {
  border-color: var(--line-2);
  transform: translateX(2px);
  box-shadow: var(--sh-sm);
}
.opt.on {
  border-color: var(--brand);
  background: rgba(99, 102, 241, 0.05);
}
.oi {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: var(--surface-3);
  color: var(--text-2);
  transition: all var(--dur-base) var(--ease-out);
}
.opt.on .oi {
  background: var(--grad-brand);
  color: #fff;
  box-shadow: var(--sh-brand);
  transform: rotate(-6deg) scale(1.05);
}
.opt b {
  font-size: 13px;
}
.opt small {
  display: block;
  font-size: 11.5px;
  color: var(--text-3);
  margin-top: 1px;
}
.tick {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--brand);
  display: grid;
  place-items: center;
  animation: pop-in var(--dur-base) var(--ease-spring) both;
}

.picker {
  margin-top: 12px;
}
.picker label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  margin-bottom: 6px;
}
.picker select {
  width: 100%;
  height: 36px;
  border-radius: var(--r-md);
  border: 1.5px solid var(--line-2);
  background: var(--surface);
  padding: 0 10px;
  font-size: 13px;
  outline: none;
}
.picker select:focus {
  border-color: var(--brand);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.14);
}

.switches {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sw-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 11px;
  border-radius: var(--r-sm);
  background: var(--surface-3);
}
.sw-row b {
  font-size: 12.5px;
}
.sw-row small {
  display: block;
  font-size: 11.5px;
  color: var(--text-3);
}

.summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 13px;
  border-radius: var(--r-md);
  background: var(--surface-3);
  margin-bottom: 12px;
}
.sm-row {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
  color: var(--text-3);
}
.sm-row b {
  color: var(--text-1);
  font-weight: 700;
}

.ok {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 11px 13px;
  border-radius: var(--r-md);
  background: linear-gradient(135deg, rgba(52, 211, 153, 0.12), rgba(34, 211, 238, 0.1));
  border: 1px solid rgba(52, 211, 153, 0.35);
}
.ok span:first-child,
.ok > svg {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #34d399, #10b981);
  flex: none;
}
.ok {
  align-items: center;
}
.ok b {
  font-size: 12.8px;
}
.ok small {
  display: block;
  font-size: 11.5px;
  color: var(--text-3);
}
.dl {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: var(--c-success);
  flex: none;
  transition: all var(--dur-fast) var(--ease-out);
}
.dl:hover {
  background: rgba(52, 211, 153, 0.14);
  transform: translateY(-1px);
}

.warn {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 11px;
  font-size: 11.8px;
  color: var(--c-warn);
  font-weight: 600;
}

.hist {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.hist li {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 12px;
  border-radius: var(--r-md);
  border: 1px solid var(--line);
  background: var(--surface-2);
  animation: fade-up var(--dur-slow) var(--ease-out) both;
  transition: transform var(--dur-base) var(--ease-out);
}
.hist li:hover {
  transform: translateX(3px);
  box-shadow: var(--sh-sm);
}
.hi {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: var(--grad-ocean);
  color: #fff;
  flex: none;
}
.hb b {
  font-size: 12.8px;
}
.hb small {
  display: block;
  font-size: 11px;
  color: var(--text-3);
}

.muted {
  font-size: 12.5px;
  color: var(--text-4);
}
</style>
