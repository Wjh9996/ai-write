import { call, openStream, type StreamHandle } from './http'
import { db, delay, delayVoid, uid } from './mock/db'
import { SEGMENT_TEXTS, randomChunk } from './mock/streamText'
import { countWords } from './chapter'
import type { GenerationEvent, GenerationJob, SegmentProgress } from '@/types'

export interface CreateJobResult {
  jobId: string
  status: GenerationJob['status']
  currentSegment: number
  totalSegments: number
}

/** mock 任务的运行时状态（真实后端时由数据库维护） */
interface JobRuntime {
  chapterId: number
  totalSegments: number
  status: GenerationJob['status']
  currentSegment: number
  cursor: number
  buffers: string[]
  started: boolean[]
  speed: number
}

const runtimes = new Map<string, JobRuntime>()
/** 结束后残留：便于 resume 时定位最后完成的完整段 */

export const TOTAL_SEGMENTS = SEGMENT_TEXTS.length

export const generationApi = {
  /** POST /api/chapters/{id}/generation-jobs */
  start(chapterId: number, idempotencyKey: string) {
    return call<CreateJobResult>('POST', `/chapters/${chapterId}/generation-jobs`, { idempotencyKey }, async () => {
      const chapter = db.chapters.find((c) => c.id === chapterId)
      if (!chapter) throw new Error('章节不存在')
      if (chapter.chapterStatus === 'GENERATING') throw new Error('本章已有进行中的生成任务')

      const jobId = uid('job')
      runtimes.set(jobId, {
        chapterId,
        totalSegments: TOTAL_SEGMENTS,
        status: 'QUEUED',
        currentSegment: 1,
        cursor: 0,
        buffers: Array(TOTAL_SEGMENTS).fill(''),
        started: Array(TOTAL_SEGMENTS).fill(false),
        speed: 1
      })
      chapter.chapterStatus = 'GENERATING'
      return delay({ jobId, status: 'QUEUED', currentSegment: 1, totalSegments: TOTAL_SEGMENTS }, 420)
    })
  },

  /** POST /api/generation-jobs/{id}/stop */
  stop(jobId: string) {
    return call<null>('POST', `/generation-jobs/${jobId}/stop`, undefined, async () => {
      const rt = runtimes.get(jobId)
      if (rt) rt.status = 'STOP_REQUESTED'
      return delayVoid(240)
    })
  },

  /** POST /api/generation-jobs/{id}/resume */
  resume(jobId: string) {
    return call<CreateJobResult>('POST', `/generation-jobs/${jobId}/resume`, undefined, async () => {
      const rt = runtimes.get(jobId)
      if (!rt) throw new Error('任务不存在，请重新开始生成')
      rt.status = 'QUEUED'
      const chapter = db.chapters.find((c) => c.id === rt.chapterId)
      if (chapter) chapter.chapterStatus = 'GENERATING'
      return delay(
        { jobId, status: 'QUEUED', currentSegment: rt.currentSegment, totalSegments: rt.totalSegments },
        320
      )
    })
  },

  /** GET /api/generation-jobs/{id} */
  status(jobId: string) {
    return call<GenerationJob>('GET', `/generation-jobs/${jobId}`, undefined, async () => {
      const rt = runtimes.get(jobId)
      if (!rt) throw new Error('任务不存在')
      return delay(
        {
          id: jobId,
          chapterId: rt.chapterId,
          status: rt.status,
          currentSegment: rt.currentSegment,
          totalSegments: rt.totalSegments,
          model: 'deepseek-chat',
          promptVersion: 'v1.3',
          errorCode: null,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        260
      )
    })
  },

  /**
   * GET /api/generation-jobs/{jobId}/events —— SSE 事件流
   * mock 模式下由本地定时器驱动，事件带单调递增 seq
   */
  openEventStream(
    jobId: string,
    handlers: {
      onStarted?: (total: number) => void
      onSegmentStart?: (seg: SegmentProgress) => void
      onDelta?: (segNo: number, text: string) => void
      onSegmentDone?: (segNo: number, written: number) => void
      onDraftReady?: (content: string, wordCount: number) => void
      onFailed?: (err: string) => void
      onStopped?: () => void
      onCompleted?: () => void
    }
  ): StreamHandle {
    const mockFactory = (emit: (e: GenerationEvent) => void, close: () => void) => {
      const rt = runtimes.get(jobId)
      if (!rt) {
        emit({ seq: 1, type: 'job.failed', payload: { message: '任务不存在' } })
        return () => {}
      }

      let seq = 0
      const send = (type: GenerationEvent['type'], payload: Record<string, unknown>) => {
        seq += 1
        emit({ seq, type, payload })
        switch (type) {
          case 'job.started':
            handlers.onStarted?.(rt.totalSegments)
            break
          case 'segment.started':
            handlers.onSegmentStart?.(payload.segment as SegmentProgress)
            break
          case 'text.delta':
            handlers.onDelta?.(payload.segmentNo as number, payload.text as string)
            break
          case 'segment.completed':
            handlers.onSegmentDone?.(payload.segmentNo as number, payload.written as number)
            break
          case 'chapter.draft_ready':
            handlers.onDraftReady?.(payload.content as string, payload.wordCount as number)
            break
          case 'job.failed':
            handlers.onFailed?.(payload.message as string)
            close()
            break
          case 'job.stopped':
            handlers.onStopped?.()
            close()
            break
          case 'job.completed':
            handlers.onCompleted?.()
            close()
            break
        }
      }

      rt.status = 'RUNNING'
      send('job.started', { jobId, chapterId: rt.chapterId, totalSegments: rt.totalSegments })

      let timer: ReturnType<typeof setTimeout> | null = null
      let cancelled = false
      let stopRequested = false

      const tickDelay = () => 26 / rt.speed + Math.random() * 18

      const runTick = () => {
        if (cancelled) return
        if (rt.status === 'STOP_REQUESTED') {
          stopRequested = true
        }
        if (stopRequested) {
          rt.status = 'STOPPED'
          const chapter = db.chapters.find((c) => c.id === rt.chapterId)
          if (chapter) chapter.chapterStatus = 'REVIEW_PENDING'
          send('job.stopped', { jobId, atSegment: rt.currentSegment })
          return
        }

        const segIdx = rt.currentSegment - 1
        const full = SEGMENT_TEXTS[segIdx % SEGMENT_TEXTS.length]

        if (!rt.started[segIdx]) {
          rt.started[segIdx] = true
          send('segment.started', {
            segment: {
              segmentNo: rt.currentSegment,
              targetWords: countWords(full),
              written: 0,
              status: 'RUNNING'
            } satisfies SegmentProgress
          })
          timer = setTimeout(runTick, 220)
          return
        }

        const { piece, next } = randomChunk(full, rt.cursor)
        rt.buffers[segIdx] += piece
        rt.cursor = next
        send('text.delta', { segmentNo: rt.currentSegment, text: piece })

        if (rt.cursor < full.length) {
          timer = setTimeout(runTick, tickDelay())
          return
        }

        // 本段完成
        const written = countWords(rt.buffers[segIdx])
        send('segment.completed', { segmentNo: rt.currentSegment, written })

        if (rt.currentSegment >= rt.totalSegments) {
          const content = rt.buffers.join('\n\n')
          const chapter = db.chapters.find((c) => c.id === rt.chapterId)
          if (chapter) {
            chapter.contentMd = content
            chapter.wordCount = countWords(content)
            chapter.chapterStatus = 'REVIEW_PENDING'
            chapter.auditStatus = 'NOT_RUN'
            chapter.contentVersion += 1
            chapter.updatedAt = new Date().toISOString()
          }
          rt.status = 'COMPLETED'
          send('chapter.draft_ready', { content, wordCount: countWords(content) })
          send('job.completed', { jobId })
          return
        }

        rt.currentSegment += 1
        rt.cursor = 0
        timer = setTimeout(runTick, 460)
      }

      timer = setTimeout(runTick, 320)

      const cancel = () => {
        cancelled = true
        if (timer) clearTimeout(timer)
        if (rt.status === 'RUNNING') rt.status = 'STOPPED'
      }

      return cancel
    }

    return openStream<GenerationEvent>({
      path: `/generation-jobs/${jobId}/events`,
      mockFactory
    })
  }
}
