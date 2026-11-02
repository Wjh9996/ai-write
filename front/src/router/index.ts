import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'workbench',
    component: () => import('@/views/WorkbenchView.vue'),
    meta: { title: '作品工作台' }
  },
  {
    path: '/novel/:novelId',
    component: () => import('@/layout/NovelLayout.vue'),
    redirect: (to) => `/novel/${String(to.params.novelId)}/outline`,
    children: [
      {
        path: 'outline',
        name: 'outline',
        component: () => import('@/views/OutlineView.vue'),
        meta: { title: '大纲', icon: 'outline' }
      },
      {
        path: 'outline/volume/:volumeId',
        name: 'volume-outline',
        component: () => import('@/views/VolumeOutlineView.vue'),
        meta: { title: '章纲确认', icon: 'outline' }
      },
      {
        path: 'settings',
        name: 'settings',
        component: () => import('@/views/SettingsView.vue'),
        meta: { title: '设定库', icon: 'settings' }
      },
      {
        path: 'write/:chapterId?',
        name: 'writer',
        component: () => import('@/views/WriterView.vue'),
        meta: { title: '章节创作', icon: 'write' }
      },
      {
        path: 'review/:chapterId?',
        name: 'review',
        component: () => import('@/views/ReviewView.vue'),
        meta: { title: '审核入库', icon: 'review' }
      },
      {
        path: 'export',
        name: 'export',
        component: () => import('@/views/ExportView.vue'),
        meta: { title: '导出', icon: 'export' }
      },
      {
        path: 'usage',
        name: 'usage',
        component: () => import('@/views/UsageView.vue'),
        meta: { title: '用量与成本', icon: 'usage' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

export const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.afterEach((to) => {
  const base = 'AI 创作台'
  document.title = to.meta?.title ? `${String(to.meta.title)} · ${base}` : base
})

export default router
