import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { novelApi, chapterApi, outlineApi, settingsApi } from '@/api'
import type {
  Chapter,
  ChapterOutline,
  CreateNovelPayload,
  Novel,
  NovelSettings,
  StyleProfile,
  Volume
} from '@/types'

export const useNovelStore = defineStore('novel', () => {
  const novels = ref<Novel[]>([])
  const current = ref<Novel | null>(null)
  const volumes = ref<Volume[]>([])
  const chapters = ref<Chapter[]>([])
  const settings = ref<NovelSettings | null>(null)
  const outlines = ref<Record<number, ChapterOutline[]>>({})

  const loadingList = ref(false)
  const loadingDetail = ref(false)

  const currentId = computed(() => current.value?.id ?? 0)

  const totalWords = computed(() =>
    novels.value.reduce((a, n) => a + (n.stats?.totalWords ?? 0), 0)
  )

  /** 当前作品的章节统计（按状态聚合） */
  const chapterGroups = computed(() => {
    const g = { APPROVED: 0, REVIEW_PENDING: 0, GENERATING: 0, OUTLINED: 0 }
    chapters.value.forEach((c) => {
      if (c.chapterStatus in g) g[c.chapterStatus as keyof typeof g] += 1
    })
    return g
  })

  /** 生成下一章的前置校验：必须前一章已审核（文档不可妥协规则 ②） */
  function canGenerate(chapter: Chapter): { ok: boolean; reason: string } {
    const prev = chapters.value.find((c) => c.chapterNo === chapter.chapterNo - 1)
    if (chapter.chapterNo > 1 && (!prev || prev.chapterStatus !== 'APPROVED')) {
      return { ok: false, reason: '前一章尚未通过审核，不能依赖它继续创作' }
    }
    if (chapter.chapterStatus === 'GENERATING') {
      return { ok: false, reason: '本章正在生成中' }
    }
    const outline = outlines.value[chapter.volumeId]?.find((o) => o.chapterNo === chapter.chapterNo)
    if (!outline) return { ok: true, reason: '' }
    if (outline.status !== 'CONFIRMED') {
      return { ok: false, reason: '本章章纲尚未确认（规则：未确认章纲不得生成）' }
    }
    return { ok: true, reason: '' }
  }

  async function loadList() {
    loadingList.value = true
    try {
      novels.value = await novelApi.list()
    } finally {
      loadingList.value = false
    }
  }

  async function enter(id: number) {
    loadingDetail.value = true
    try {
      const [novel, vs, cs, ss] = await Promise.all([
        novelApi.detail(id),
        outlineApi.volumes(id),
        chapterApi.list(id),
        settingsApi.get(id)
      ])
      current.value = novel
      volumes.value = vs
      chapters.value = cs
      settings.value = ss
    } finally {
      loadingDetail.value = false
    }
  }

  async function loadVolumes() {
    if (!currentId.value) return
    volumes.value = await outlineApi.volumes(currentId.value)
  }

  async function loadChapters() {
    if (!currentId.value) return
    chapters.value = await chapterApi.list(currentId.value)
  }

  async function loadOutlines(volumeId: number) {
    outlines.value[volumeId] = await outlineApi.outlines(volumeId)
    return outlines.value[volumeId]
  }

  async function create(payload: CreateNovelPayload) {
    const novel = await novelApi.create(payload)
    novels.value.unshift(novel)
    return novel
  }

  async function remove(id: number) {
    await novelApi.remove(id)
    novels.value = novels.value.filter((n) => n.id !== id)
  }

  async function saveMainOutline(content: string) {
    if (!current.value) return
    current.value = await novelApi.updateOutline(current.value.id, content)
  }

  async function importTxt(file: File) {
    if (!current.value) return
    current.value = await novelApi.importTxt(current.value.id, file)
    await loadVolumes()
  }

  function setStyle(style: StyleProfile) {
    if (settings.value) settings.value.style = style
  }

  function patchChapter(id: number, patch: Partial<Chapter>) {
    const idx = chapters.value.findIndex((c) => c.id === id)
    if (idx >= 0) chapters.value[idx] = { ...chapters.value[idx], ...patch }
  }

  return {
    novels,
    current,
    volumes,
    chapters,
    settings,
    outlines,
    loadingList,
    loadingDetail,
    currentId,
    totalWords,
    chapterGroups,
    canGenerate,
    loadList,
    enter,
    loadVolumes,
    loadChapters,
    loadOutlines,
    create,
    remove,
    saveMainOutline,
    importTxt,
    setStyle,
    patchChapter
  }
})
