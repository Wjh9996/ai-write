<script setup lang="ts">
/** 作品工作台：总览统计 + 作品卡片 + 新建/导入 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'
import StatCard from '@/components/common/StatCard.vue'
import AppButton from '@/components/common/AppButton.vue'
import AppCard from '@/components/common/AppCard.vue'
import AppIcon from '@/components/common/AppIcon.vue'
import AppTag from '@/components/common/AppTag.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppTextarea from '@/components/common/AppTextarea.vue'
import AppSkeleton from '@/components/common/AppSkeleton.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import AppProgress from '@/components/common/AppProgress.vue'

const router = useRouter()
const novel = useNovelStore()
const user = useUserStore()
const ui = useUiStore()

const creating = ref(false)
const form = ref({ title: '', intro: '', mainOutline: '' })
const submitting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const keyword = ref('')

onMounted(async () => {
  await user.load()
  await novel.loadList()
})

const STATUS: Record<string, { label: string; tone: 'green' | 'amber' | 'gray' | 'blue' }> = {
  WRITING: { label: '连载中', tone: 'green' },
  DRAFT: { label: '草稿', tone: 'gray' },
  PAUSED: { label: '暂停', tone: 'amber' },
  FINISHED: { label: '已完结', tone: 'blue' }
}

const list = computed(() => {
  const k = keyword.value.trim()
  if (!k) return novel.novels
  return novel.novels.filter((n) => n.title.includes(k) || n.intro.includes(k))
})

const totals = computed(() => {
  const t = {
    words: 0,
    chapters: 0,
    approved: 0,
    pending: 0
  }
  novel.novels.forEach((n) => {
    t.words += n.stats?.totalWords ?? 0
    t.chapters += n.stats?.chapterCount ?? 0
    t.approved += n.stats?.approvedCount ?? 0
    t.pending += n.stats?.reviewPendingCount ?? 0
  })
  return t
})

async function submitCreate() {
  if (!form.value.title.trim()) {
    ui.warn('先给作品起个名字')
    return
  }
  submitting.value = true
  try {
    const n = await novel.create({
      title: form.value.title.trim(),
      intro: form.value.intro,
      mainOutline: form.value.mainOutline
    })
    creating.value = false
    form.value = { title: '', intro: '', mainOutline: '' }
    ui.success('作品已创建', '下一步：进入大纲页导入你的总纲')
    router.push(`/novel/${n.id}/outline`)
  } catch (e) {
    ui.error('创建失败', (e as Error).message)
  } finally {
    submitting.value = false
  }
}

function triggerImport() {
  fileInput.value?.click()
}

async function onFile(ev: Event) {
  const el = ev.target as HTMLInputElement
  const file = el.files?.[0]
  if (!file) return
  if (/\.docx?$|\.pdf$/.test(file.name)) {
    ui.warn('首版只支持 TXT 大纲导入', '请把 docx / pdf 转成纯文本后再试')
    el.value = ''
    return
  }
  ui.showLoading('正在解析大纲…')
  try {
    const text = await file.text()
    form.value.mainOutline = `[导入自 ${file.name}]\n${text}`
    ui.success('大纲已读入', `${file.name} · ${text.length} 字，填写作品名后创建即可`)
  } catch {
    ui.error('读取失败', '文件不是纯文本或编码不支持')
  } finally {
    ui.hideLoading()
    el.value = ''
  }
}

async function removeNovel(id: number, title: string) {
  await novel.remove(id)
  ui.success('已删除', title)
}
</script>

<template>
  <div class="page">
    <header class="ph">
      <div class="ph-left">
        <h1>
          <span class="hi">{{ user.profile?.nickname || '作者' }}</span
          >，今天准备推进哪一章？
        </h1>
        <p>共 {{ novel.novels.length }} 部作品 · 累计 {{ totals.words.toLocaleString('zh-CN') }} 字</p>
      </div>
      <div class="ph-right">
        <AppButton variant="outline" icon="upload" @click="triggerImport">导入 TXT 大纲</AppButton>
        <AppButton variant="primary" icon="plus" @click="creating = true">新建作品</AppButton>
      </div>
      <input ref="fileInput" type="file" accept=".txt,text/plain" hidden @change="onFile" />
    </header>

    <!-- 总览 -->
    <section class="stats">
      <StatCard label="累计字数" :value="totals.words" suffix="字" icon="book" tone="brand" :delay="40" />
      <StatCard
        label="已审核章节"
        :value="totals.approved"
        suffix="章"
        icon="shield"
        tone="mint"
        :progress="totals.chapters ? (totals.approved / totals.chapters) * 100 : 0"
        hint="已入库的章节可用于后续上下文"
        :delay="100"
      />
      <StatCard
        label="待处理"
        :value="totals.pending"
        suffix="章"
        icon="clock"
        tone="sunset"
        hint="等待你确认入库"
        :delay="160"
      />
      <StatCard
        label="本月用量"
        :value="user.profile ? Number((user.profile.usedChars / 10000).toFixed(1)) : 0"
        suffix="万字"
        icon="coin"
        tone="violet"
        :progress="user.quotaPercent"
        hint="额度目标：每月 50—100 元"
        :delay="220"
      />
    </section>

    <!-- 作品列表 -->
    <AppCard title="我的作品" desc="点击进入大纲 / 创作 / 审核" icon="folder" tone="brand">
      <template #actions>
        <div class="search">
          <AppIcon name="search" :size="15" color="var(--text-4)" />
          <input v-model="keyword" placeholder="搜索作品名或简介" />
        </div>
      </template>

      <div v-if="novel.loadingList" class="grid">
        <AppCard v-for="i in 3" :key="i" flat>
          <AppSkeleton variant="title" />
          <div style="height: 10px" />
          <AppSkeleton :lines="2" />
        </AppCard>
      </div>

      <div v-else-if="!list.length" class="empty-wrap">
        <AppEmpty
          title="还没有作品"
          desc="新建一部作品，或把写好的 TXT 总纲导进来，AI 会帮你拆成可以直接写的一章一章"
          icon="folder"
        >
          <AppButton variant="primary" icon="plus" @click="creating = true">新建作品</AppButton>
        </AppEmpty>
      </div>

      <TransitionGroup v-else name="list" tag="div" class="grid">
        <article v-for="(n, i) in list" :key="n.id" class="novel-card" :style="{ animationDelay: `${i * 70}ms` }">
          <span class="cover" :style="{ background: n.coverGradient }" aria-hidden="true">
            <AppIcon name="book" :size="20" color="rgba(255,255,255,.95)" />
          </span>

          <div class="body">
            <div class="title-row">
              <h3>{{ n.title }}</h3>
              <AppTag :label="STATUS[n.status]?.label ?? '草稿'" :tone="STATUS[n.status]?.tone ?? 'gray'" dot />
            </div>
            <p class="intro">{{ n.intro || '（还没有写简介）' }}</p>

            <div class="metrics">
              <span><b class="mono">{{ (n.stats?.totalWords ?? 0).toLocaleString('zh-CN') }}</b>字</span>
              <span><b class="mono">{{ n.stats?.approvedCount ?? 0 }}</b>/{{ n.stats?.chapterCount ?? 0 }} 章</span>
              <span><b class="mono">{{ n.stats?.volumeCount ?? 0 }}</b> 卷</span>
            </div>

            <AppProgress
              :value="n.stats?.approvedCount ?? 0"
              :max="Math.max(1, n.stats?.chapterCount ?? 1)"
              :height="6"
              striped
            />
          </div>

          <footer class="acts">
            <AppButton size="sm" variant="primary" icon="arrowRight" @click="router.push(`/novel/${n.id}/outline`)">
              进入创作
            </AppButton>
            <AppButton size="sm" variant="ghost" icon="trash" @click="removeNovel(n.id, n.title)">删除</AppButton>
          </footer>

          <i class="shimmer" aria-hidden="true" />
        </article>
      </TransitionGroup>
    </AppCard>

    <!-- 新建弹窗 -->
    <AppModal
      v-model="creating"
      title="新建作品"
      desc="先有作品容器，之后所有大纲、设定、章节都挂在它下面"
      icon="plus"
      :width="560"
    >
      <div class="form">
        <AppInput v-model="form.title" label="作品名" placeholder="例如：御兽：从一枚灰蛋开始" :maxlength="40" />
        <AppInput v-model="form.intro" label="一句话简介" placeholder="给未来的自己看的一句话" :maxlength="60" />
        <div class="field">
          <label>总纲（可留白，之后在大纲页补）</label>
          <AppTextarea v-model="form.mainOutline" :rows="8" placeholder="粘贴主线，或点击「导入 TXT 大纲」" show-count />
        </div>
      </div>
      <template #footer>
        <AppButton variant="ghost" @click="creating = false">取消</AppButton>
        <AppButton variant="primary" icon="check" :loading="submitting" @click="submitCreate">创建并进入</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.page {
  padding: 26px 28px 60px;
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.ph {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  animation: fade-up 500ms var(--ease-out) both;
}
.ph h1 {
  font-size: 23px;
  font-weight: 800;
  letter-spacing: -0.3px;
}
.hi {
  background: var(--grad-brand);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.ph p {
  font-size: 13px;
  color: var(--text-3);
  margin-top: 3px;
}
.ph-right {
  display: flex;
  gap: 10px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
  margin-top: 4px;
}

.novel-card {
  position: relative;
  display: grid;
  grid-template-columns: 62px 1fr;
  grid-template-areas: 'cover body' 'acts acts';
  gap: 12px 14px;
  padding: 15px;
  border-radius: var(--r-lg);
  border: 1px solid var(--line);
  background: var(--surface);
  box-shadow: var(--sh-xs);
  overflow: hidden;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
  transition: transform var(--dur-base) var(--ease-out), box-shadow var(--dur-base) var(--ease-out);
}
.novel-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--sh-lg);
}

.cover {
  grid-area: cover;
  width: 62px;
  height: 62px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  box-shadow: var(--sh-sm);
  transition: transform var(--dur-base) var(--ease-spring);
}
.novel-card:hover .cover {
  transform: rotate(-6deg) scale(1.05);
}

.body {
  grid-area: body;
  min-width: 0;
}
.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
}
.title-row h3 {
  font-size: 15px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.intro {
  font-size: 12.5px;
  color: var(--text-3);
  margin: 4px 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 34px;
}
.metrics {
  display: flex;
  gap: 12px;
  font-size: 11.5px;
  color: var(--text-3);
  margin-bottom: 7px;
}
.metrics b {
  color: var(--text-1);
  font-size: 13px;
}

.acts {
  grid-area: acts;
  display: flex;
  gap: 8px;
  padding-top: 11px;
  border-top: 1px dashed var(--line-2);
}

.shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, transparent 35%, rgba(255, 255, 255, 0.5) 50%, transparent 65%);
  background-size: 220% 100%;
  transform: translateX(-120%);
  opacity: 0;
  pointer-events: none;
}
.novel-card:hover .shimmer {
  opacity: 1;
  animation: shimmer 1.1s var(--ease-out);
}

.empty-wrap {
  padding: 20px 0;
}

.search {
  display: flex;
  align-items: center;
  gap: 7px;
  height: 32px;
  padding: 0 11px;
  border-radius: var(--r-full);
  background: var(--surface-3);
  border: 1px solid var(--line);
  transition: border-color var(--dur-base) var(--ease-out);
}
.search:focus-within {
  border-color: var(--brand);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.12);
}
.search input {
  border: none;
  outline: none;
  background: transparent;
  width: 150px;
  font-size: 12.5px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.field label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  margin-bottom: 6px;
}
</style>
