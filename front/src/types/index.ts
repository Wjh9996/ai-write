/**
 * 领域模型定义
 * 对齐《设计文档NEW.md》第 5 节数据库设计与第 6 节接口设计
 */

/** 统一响应包装 {code,message,data,requestId} */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  data: T
  requestId: string
}

// ---------------- 账号 ----------------
export interface UserProfile {
  id: number
  email: string
  nickname: string
  avatarColor: string
  plan: 'FREE' | 'PRO'
  monthlyQuotaChars: number
  usedChars: number
  /** 真实后端登录返回，前端负责存到 localStorage */
  token?: string
}

export interface LoginPayload {
  email: string
  password: string
}

// ---------------- 作品 ----------------
export type NovelStatus = 'DRAFT' | 'WRITING' | 'PAUSED' | 'FINISHED'

export interface Novel {
  id: number
  userId: number
  title: string
  intro: string
  mainOutline: string
  coverGradient: string
  status: NovelStatus
  version: number
  createdAt: string
  updatedAt: string
  /** 派生统计（列表接口返回） */
  stats?: NovelStats
}

export interface NovelStats {
  totalWords: number
  chapterCount: number
  approvedCount: number
  reviewPendingCount: number
  generatingCount: number
  outlinedCount: number
  volumeCount: number
}

export interface CreateNovelPayload {
  title: string
  intro?: string
  mainOutline?: string
}

// ---------------- 卷 / 大纲 ----------------
export type VolumeStatus = 'RAW' | 'OUTLINED' | 'PENDING_CONFIRM' | 'CONFIRMED'

export interface Volume {
  id: number
  novelId: number
  volumeNo: number
  title: string
  /** 有序阶段事件序列 */
  events: VolumeEvent[]
  status: VolumeStatus
  targetChapters?: number
  createdAt: string
}

export interface VolumeEvent {
  id: number
  sort: number
  content: string
}

export type OutlineStatus = 'DRAFT' | 'CONFIRMED'

export interface ChapterOutline {
  id: number
  volumeId: number
  chapterNo: number
  title: string
  beats: string[]
  characters: string[]
  estimatedWords: number
  status: OutlineStatus
  version: number
}

/** AI 拆章纲返回体，含 proposed_entities */
export interface OutlineGenerationResult {
  volumeId: number
  outlines: ChapterOutline[]
  proposedEntities: ProposedEntity[]
}

export interface ProposedEntity {
  id: string
  type: EntityType
  name: string
  reason: string
}

// ---------------- 设定 ----------------
export type EntityType = 'CHARACTER' | 'PLACE' | 'ORGANIZATION' | 'ITEM'
export type SourceType = 'AI' | 'MANUAL'
export type ApprovalStatus = 'DRAFT' | 'CONFIRMED' | 'REJECTED'

export interface SettingEntity {
  id: number
  novelId: number
  type: EntityType
  name: string
  aliases: string[]
  /** 角色：身份/性格/外貌/口癖/当前等级 等自由字段 */
  card: Record<string, string>
  source: SourceType
  approvalStatus: ApprovalStatus
  /** 首次揭露章号，用于防止后文提前泄露 */
  revealChapter: number | null
  updatedAt: string
}

export interface WorldSetting {
  id: number
  novelId: number
  category: string
  title: string
  content: string
  source: SourceType
  approvalStatus: ApprovalStatus
}

export interface TimelineEvent {
  id: number
  novelId: number
  chapterId: number | null
  timeLabel: string
  event: string
  approvalStatus: ApprovalStatus
  sort: number
}

export type ForeshadowStatus = 'PLANTED' | 'PAID_OFF'

export interface Foreshadowing {
  id: number
  novelId: number
  content: string
  setChapterId: number | null
  payoffChapterId: number | null
  status: ForeshadowStatus
  source: SourceType
  approvalStatus: ApprovalStatus
}

export interface StyleProfile {
  id: number
  novelId: number
  /** 人称 / 视角 / 节奏 / 爽点 / 对话比例 / 禁用内容 等 */
  card: Record<string, string>
  sampleText: string
  forbiddenTerms: string[]
}

export interface NovelSettings {
  entities: SettingEntity[]
  worlds: WorldSetting[]
  timeline: TimelineEvent[]
  foreshadowings: Foreshadowing[]
  style: StyleProfile | null
}

// ---------------- 章节 ----------------
export type ChapterStatus = 'OUTLINED' | 'GENERATING' | 'REVIEW_PENDING' | 'APPROVED'
export type AuditStatus = 'NOT_RUN' | 'DRAFT' | 'PASSED' | 'NEEDS_REVIEW'

export interface Chapter {
  id: number
  novelId: number
  volumeId: number
  outlineId: number | null
  chapterNo: number
  title: string
  contentMd: string
  wordCount: number
  chapterStatus: ChapterStatus
  auditStatus: AuditStatus
  contentVersion: number
  createdAt: string
  updatedAt: string
}

export interface ChapterVersion {
  id: number
  chapterId: number
  versionNo: number
  contentMd: string
  editorType: 'AI' | 'HUMAN'
  createdAt: string
}

// ---------------- 生成任务 ----------------
export type JobStatus =
  | 'QUEUED'
  | 'RUNNING'
  | 'STOP_REQUESTED'
  | 'STOPPED'
  | 'FAILED'
  | 'COMPLETED'

export interface GenerationJob {
  id: string
  chapterId: number
  status: JobStatus
  currentSegment: number
  totalSegments: number
  model: string
  promptVersion: string
  errorCode: string | null
  createdAt: string
  updatedAt: string
}

export interface SegmentPlan {
  segmentNo: number
  beats: string[]
  targetWords: number
}

export interface GenerationSegment {
  id: number
  jobId: string
  segmentNo: number
  plan: SegmentPlan
  content: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'INCOMPLETE' | 'FAILED'
}

/** SSE 事件类型 */
export type GenerationEventType =
  | 'job.started'
  | 'segment.started'
  | 'text.delta'
  | 'segment.completed'
  | 'chapter.draft_ready'
  | 'job.failed'
  | 'job.stopped'
  | 'job.completed'

export interface GenerationEvent {
  seq: number
  type: GenerationEventType
  payload: Record<string, unknown>
}

export interface SegmentProgress {
  segmentNo: number
  targetWords: number
  written: number
  status: string
}

// ---------------- 审核 ----------------
export interface Issue {
  id: string
  type: '角色' | '地名' | '设定冲突' | '时间线' | '视角' | '越界' | '伏笔'
  level: 'HIGH' | 'MID' | 'LOW'
  quote: string
  issue: string
  suggest: string
  resolved: boolean
}

export interface SettingChangeDraft {
  id: string
  kind: 'CHARACTER_UPDATE' | 'TIMELINE' | 'FORESHADOWING' | 'NEW_ENTITY'
  name: string
  change: string
  time?: string
  action?: '埋' | '收'
  entityType?: EntityType
  accepted: boolean
  /** 人工是否编辑过 */
  edited?: boolean
}

export interface SummaryDraft {
  content: string
  sourceContentVersion: number
}

export interface ReviewDraft {
  chapterId: number
  issues: Issue[]
  summary: SummaryDraft
  changes: SettingChangeDraft[]
  wordCount: number
  createdAt: string
}

// ---------------- 用量 ----------------
export interface UsageRecord {
  id: number
  jobId: string
  scene: string
  model: string
  inputTokens: number
  outputTokens: number
  estimatedCost: number
  createdAt: string
}

export interface UsageOverview {
  monthCost: number
  monthTokens: number
  chapterThisMonth: number
  budget: number
  byScene: { scene: string; cost: number; times: number }[]
  records: UsageRecord[]
}

// ---------------- 导出 ----------------
export type ExportScope = 'CHAPTER' | 'VOLUME' | 'BOOK'
export type ExportFormat = 'docx' | 'md' | 'txt'

export interface ExportOption {
  scope: ExportScope
  format: ExportFormat
  withTitle: boolean
  withAuthor: boolean
}
