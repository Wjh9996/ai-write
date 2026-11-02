import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { generationApi, countWords } from '@/api'
import type { SegmentProgress } from '@/types'
import type { StreamHandle } from '@/api/http'

export type StreamStatus = 'IDLE' | 'CONNECTING' | 'RUNNING' | 'STOPPED' | 'FAILED' | 'COMPLETED'

export const useGenerationStore = defineStore('generation', () => {
  const jobId = ref('')
  const chapterId = ref(0)
  const status = ref<StreamStatus>('IDLE')
  const totalSegments = ref(5)
  const currentSegment = ref(0)
  const segments = ref<SegmentProgress[]>([])
  /** 分段文本，key = segmentNo */
  const buffers = ref<Record<number, string>>({})
  const errorMessage = ref('')
  let handle: StreamHandle | null = null

  const mergedText = computed(() =>
    Object.keys(buffers.value)
      .map(Number)
      .sort((a, b) => a - b)
      .map((k) => buffers.value[k])
      .join('\n\n')
  )

  const wordCount = computed(() => countWords(mergedText.value))
  const targetWords = computed(() => 2500)
  const percent = computed(() => Math.min(100, Math.round((wordCount.value / targetWords.value) * 100)))
  const isRunning = computed(() => status.value === 'RUNNING' || status.value === 'CONNECTING')

  function reset() {
    handle?.close()
    handle = null
    jobId.value = ''
    status.value = 'IDLE'
    currentSegment.value = 0
    segments.value = []
    buffers.value = {}
    errorMessage.value = ''
  }

  /** 建立 SSE 连接 */
  function attach(cid: number, jid: string) {
    chapterId.value = cid
    jobId.value = jid
    status.value = 'CONNECTING'
    errorMessage.value = ''

    handle = generationApi.openEventStream(jid, {
      onStarted: (total) => {
        totalSegments.value = total
        status.value = 'RUNNING'
      },
      onSegmentStart: (seg) => {
        currentSegment.value = seg.segmentNo
        const idx = segments.value.findIndex((s) => s.segmentNo === seg.segmentNo)
        if (idx >= 0) segments.value[idx] = seg
        else segments.value.push(seg)
      },
      onDelta: (segNo, text) => {
        buffers.value[segNo] = (buffers.value[segNo] ?? '') + text
        const idx = segments.value.findIndex((s) => s.segmentNo === segNo)
        if (idx >= 0) segments.value[idx].written = countWords(buffers.value[segNo])
      },
      onSegmentDone: (segNo, written) => {
        const idx = segments.value.findIndex((s) => s.segmentNo === segNo)
        if (idx >= 0) {
          segments.value[idx].status = 'COMPLETED'
          segments.value[idx].written = written
        }
      },
      onDraftReady: () => {
        status.value = 'COMPLETED'
      },
      onCompleted: () => {
        status.value = 'COMPLETED'
      },
      onStopped: () => {
        status.value = 'STOPPED'
        segments.value.forEach((s) => {
          if (s.status === 'RUNNING') s.status = 'INCOMPLETE'
        })
      },
      onFailed: (msg) => {
        status.value = 'FAILED'
        errorMessage.value = msg
      }
    })
  }

  async function start(cid: number) {
    reset()
    const res = await generationApi.start(cid, `key-${cid}-${Date.now()}`)
    attach(cid, res.jobId)
    return res
  }

  async function stop() {
    if (!jobId.value) return
    await generationApi.stop(jobId.value)
    handle?.close()
    handle = null
    status.value = 'STOPPED'
    segments.value.forEach((s) => {
      if (s.status === 'RUNNING') s.status = 'INCOMPLETE'
    })
  }

  async function resume() {
    if (!jobId.value) return
    const res = await generationApi.resume(jobId.value)
    attach(chapterId.value, res.jobId)
  }

  function close() {
    handle?.close()
    handle = null
  }

  return {
    jobId,
    chapterId,
    status,
    totalSegments,
    currentSegment,
    segments,
    buffers,
    errorMessage,
    mergedText,
    wordCount,
    targetWords,
    percent,
    isRunning,
    start,
    stop,
    resume,
    reset,
    close
  }
})
