<script setup lang="ts">
/** 作品级布局：顶部条 + 侧边导航 + 内容区 */
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useNovelStore } from '@/stores/novel'
import { useUserStore } from '@/stores/user'
import { useUiStore } from '@/stores/ui'
import AppIcon from '@/components/common/AppIcon.vue'
import AppProgress from '@/components/common/AppProgress.vue'

const route = useRoute()
const router = useRouter()
const novel = useNovelStore()
const user = useUserStore()
const ui = useUiStore()

const novelId = computed(() => Number(route.params.novelId))

const NAV = [
  { key: 'outline', label: '大纲', icon: 'outline', to: (id: number) => `/novel/${id}/outline` },
  { key: 'settings', label: '设定库', icon: 'settings', to: (id: number) => `/novel/${id}/settings` },
  { key: 'writer', label: '章节创作', icon: 'write', to: (id: number) => `/novel/${id}/write` },
  { key: 'review', label: '审核入库', icon: 'review', to: (id: number) => `/novel/${id}/review` },
  { key: 'export', label: '导出', icon: 'export', to: (id: number) => `/novel/${id}/export` },
  { key: 'usage', label: '用量与成本', icon: 'usage', to: (id: number) => `/novel/${id}/usage` }
]

const progress = computed(() => {
  const s = novel.current?.stats
  if (!s || !s.chapterCount) return 0
  return Math.round((s.approvedCount / s.chapterCount) * 100)
})

async function load() {
  if (!novelId.value) return
  try {
    await novel.enter(novelId.value)
  } catch (e) {
    ui.error('作品加载失败', (e as Error).message)
    router.push('/')
  }
}

onMounted(load)
watch(novelId, load)

function goHome() {
  router.push('/')
}

async function logout() {
  await user.logout()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <!-- 顶部条 -->
    <header class="topbar">
      <button class="brand" @click="goHome">
        <span class="logo"><AppIcon name="write" :size="15" color="#fff" /></span>
        <span class="name">AI 创作台</span>
      </button>

      <span class="slash"><AppIcon name="chevronRight" :size="14" color="var(--text-4)" /></span>

      <div class="novel-title">
        <h2>{{ novel.current?.title ?? '加载中…' }}</h2>
        <span v-if="novel.current?.stats" class="meta mono">
          {{ novel.current.stats.totalWords.toLocaleString('zh-CN') }} 字 ·
          {{ novel.current.stats.approvedCount }}/{{ novel.current.stats.chapterCount }} 章已审核
        </span>
      </div>

      <div class="spacer" />

      <div class="prog">
        <AppProgress :value="progress" :size="34" :thickness="4" type="ring" :show-label="true" />
        <span class="pl">审核进度</span>
      </div>

      <div class="user">
        <span class="avatar">{{ user.profile?.nickname?.slice(0, 1) ?? 'U' }}</span>
        <div class="who">
          <b>{{ user.profile?.nickname }}</b>
          <small>{{ user.profile?.plan === 'PRO' ? '专业版' : '免费版' }}</small>
        </div>
        <button class="out" title="退出登录" @click="logout">
          <AppIcon name="logout" :size="15" />
        </button>
      </div>
    </header>

    <div class="main">
      <!-- 侧边导航 -->
      <aside class="sidenav">
        <nav>
          <RouterLink
            v-for="(n, i) in NAV"
            :key="n.key"
            :to="n.to(novelId)"
            class="nav-item"
            :class="{ on: route.name === n.key || (n.key === 'outline' && route.name === 'volume-outline') }"
            :style="{ animationDelay: `${i * 50}ms` }"
          >
            <AppIcon :name="n.icon" :size="17" />
            <span>{{ n.label }}</span>
            <i class="pill" aria-hidden="true" />
          </RouterLink>
        </nav>

        <div class="side-foot">
          <div class="quota">
            <div class="q-head">
              <span>本月额度</span>
              <b class="mono">{{ user.quotaPercent }}%</b>
            </div>
            <AppProgress :value="user.quotaPercent" :height="6" striped tone="236,72,153" />
            <small class="mono">
              {{ (user.profile?.usedChars ?? 0).toLocaleString('zh-CN') }} /
              {{ (user.profile?.monthlyQuotaChars ?? 0).toLocaleString('zh-CN') }} 字
            </small>
          </div>
        </div>
      </aside>

      <!-- 内容区 -->
      <main class="content">
        <RouterView v-slot="{ Component }">
          <Transition name="view" mode="out-in">
            <Suspense>
              <component :is="Component" :key="route.fullPath" />
            </Suspense>
          </Transition>
        </RouterView>
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

/* 顶栏 */
.topbar {
  height: var(--topbar-h);
  flex: none;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  background: var(--surface-glass);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid var(--line);
  z-index: 20;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
}
.logo {
  width: 28px;
  height: 28px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: var(--grad-brand);
  box-shadow: var(--sh-brand);
  transition: transform var(--dur-base) var(--ease-spring);
}
.brand:hover .logo {
  transform: rotate(-10deg) scale(1.08);
}
.name {
  font-weight: 800;
  font-size: 14.5px;
  background: var(--grad-brand);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.slash {
  display: grid;
  place-items: center;
  color: var(--text-4);
}

.novel-title {
  min-width: 0;
}
.novel-title h2 {
  font-size: 14.5px;
  font-weight: 700;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}
.novel-title .meta {
  font-size: 11px;
  color: var(--text-3);
}

.spacer {
  flex: 1;
}

.prog {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-right: 14px;
  border-right: 1px solid var(--line);
}
.pl {
  font-size: 11.5px;
  color: var(--text-3);
  font-weight: 600;
}

.user {
  display: flex;
  align-items: center;
  gap: 9px;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
  font-size: 14px;
  background: var(--grad-brand);
  box-shadow: var(--sh-brand);
}
.who b {
  display: block;
  font-size: 13px;
  line-height: 1.2;
}
.who small {
  font-size: 10.5px;
  color: var(--text-3);
}
.out {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  color: var(--text-3);
  transition: all var(--dur-fast) var(--ease-out);
}
.out:hover {
  background: rgba(239, 68, 68, 0.1);
  color: var(--c-danger);
}

/* 主体 */
.main {
  flex: 1;
  display: flex;
  min-height: 0;
}

.sidenav {
  width: var(--nav-w);
  flex: none;
  background: var(--surface);
  border-right: 1px solid var(--line);
  display: flex;
  flex-direction: column;
  padding: 14px 12px;
  gap: 10px;
}

.sidenav nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border-radius: var(--r-md);
  color: var(--text-2);
  font-size: 13.5px;
  font-weight: 600;
  overflow: hidden;
  animation: fade-up var(--dur-slow) var(--ease-out) both;
  transition: all var(--dur-base) var(--ease-out);
}

.nav-item:hover {
  background: var(--surface-3);
  color: var(--text-1);
  transform: translateX(2px);
}

.nav-item.on {
  color: var(--brand-strong);
  background: rgba(99, 102, 241, 0.09);
}

.nav-item.on .pill {
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: var(--r-full);
  background: var(--grad-brand);
  animation: fade-in var(--dur-base) var(--ease-out) both;
}

.side-foot {
  margin-top: auto;
}

.quota {
  padding: 12px;
  border-radius: var(--r-md);
  background: linear-gradient(160deg, #fdfbff, #f6f4ff);
  border: 1px solid var(--line);
}
.q-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11.5px;
  color: var(--text-3);
  font-weight: 600;
  margin-bottom: 6px;
}
.q-head b {
  color: var(--pink);
}
.quota small {
  display: block;
  margin-top: 5px;
  font-size: 10.5px;
  color: var(--text-4);
}

.content {
  flex: 1;
  min-width: 0;
  overflow: auto;
  position: relative;
}
</style>
