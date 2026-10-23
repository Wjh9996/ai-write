# AI 长篇小说创作系统设计报告（Java 实现方案）

版本：1.0（面向个人项目 MVP）  
依据：《设计思考.md》《测试纲要.md》《测试提示词.md》。本文的 Java 架构、接口及工程约束属于实现建议；测试小说《御兽：从一枚灰蛋开始》为验证语料，不是平台固定业务内容。

## 1. 项目概述

### 1.1 定位与目标
面向网文作者的自动生成兼辅助编辑工具。用户导入 TXT 总纲，维护总纲—卷纲—章纲，AI 按卷拆章纲并由人确认；每次仅生成一章，约 2500 中文字（验收区间 2300—2700），逐句/逐块流式展示；用户可在 Markdown 编辑器修改，审核后才将摘要和设定变化写入后续创作上下文。首版仅 Web，预留 App/小程序 API。个人月度 API 预算目标 50—100 元，需以实际模型价格与 token 用量验证，不把材料中的估算视为报价。

### 1.2 范围
P0：账号、作品管理、TXT 导入、三级大纲、角色/世界观/时间线/伏笔/文风卡、章纲确认、单章分段生成、SSE 流式显示、停止/失败恢复、Markdown 编辑与版本、AI 一致性检查、人工确认回写、DOCX 导出及用量记录。P1：局部重写、多版本比较、批量拆卷、成本报警。后续：订阅支付、移动端、小程序、公开发布、自动合规审核；不纳入 MVP。

## 2. 需求与业务规则

角色：普通作者（首版可仅单用户，但保留 user_id 隔离）；未来管理员。核心用例：创建作品→输入总纲与阶段事件→AI 抽取设定初稿→人工修订→按卷拆成约 15—40 个章纲，每章 3—7 个 beat→人工确认→逐章生成→编辑→自检与摘要草稿→人工审核→确认入库→下一章。支持第三人称限知、中文网文风格、章节标题和卷标题导出。

不可妥协的规则：①未确认章纲不得生成；②每章审核后才允许依赖其事实生成下一章；③只有 confirmed 摘要/设定更新进入后续上下文；④正文以 chapters.content_md 为唯一当前权威版本；⑤AI 不得直接覆盖人工编辑；⑥禁止将后续剧情真相泄漏到当前章节可见知识中；⑦一章仅允许一个活动生成任务；⑧所有写入按 user_id + novel_id 做权限校验。

## 3. 技术架构与选型（建议）

- 后端：Java 21 LTS、Spring Boot 3.x、Spring Web、Spring Security、Bean Validation、Spring Data JPA 或 MyBatis（二选一；首版推荐 MyBatis）、Flyway。
- AI 接入：独立 ModelGateway 接口，使用 HTTP 客户端调用兼容的大模型 API；支持流式输出、超时、重试、模型/价格配置，不绑定单一厂商。
- 数据：PostgreSQL（JSONB 存卡片与结构化草稿）；首版无需向量数据库。文件存储本地受控目录，未来换对象存储。
- 前端：Vue 3 + TypeScript + Markdown 编辑器；SSE/EventSource 或 fetch 流式消费。Java 提供 REST + SSE；Word 导出用 Apache POI。
- 部署：单体模块化应用 + PostgreSQL + 反向代理，Docker Compose；首版不引入微服务、MQ、Redis。需要多实例或异步规模增长时再升级。

```text
Web(Vue3/Markdown)
  | HTTPS REST / SSE
Spring Boot API (Auth / Novel / Outline / Settings / Chapter / Export)
  |-- CreationOrchestrator -> ContextBuilder -> PromptRenderer -> ModelGateway
  |                         -> SegmentStore -> Merge -> ConsistencyCheck
  |                         -> SummaryDraft -> HumanApproval
  |-- PostgreSQL (business records, job state, snapshots, usage)
  |-- File storage (import/export)
```

建议 Java 包结构：`com.example.novel.{auth,novel,outline,setting,chapter,generation,context,audit,export,usage,common}`，每模块按 controller/application/domain/infrastructure 分层。Controller 不直接拼提示词或调用模型。

## 4. 核心模块详细设计

### 4.1 大纲与设定
Novel 包含总纲；Volume 保存有序阶段事件；ChapterOutline 保存章名、3—7 beats、涉及角色、预计字数、确认版本。AI 拆章纲返回 JSON，经 JSON Schema/Java DTO 校验：章号连续、beat 数合规、涉及角色在已知实体内；新实体放 `proposed_entities`，不得默默入库。设定卡包括角色、地点、组织、物品、世界规则、时间线、伏笔、文风。区分 AI 草稿与人工确认；允许存“作者可知但当前角色不可知”的秘密及首次揭露章号。

### 4.2 上下文管理（核心）
`ContextBuilder.build(novelId, chapterId, segmentNo)` 按以下顺序组包：固定系统约束与文风卡→本章已确认章纲/本段 beat→相关且在当前章可见的角色、世界观、时间线与伏笔→已确认的全书/本卷压缩摘要及最近 3 章摘要→上一段末尾约 200 字→禁止项与本段目标。按模型 tokenizer 预算 token，给输出预留空间；超预算优先压缩旧摘要，不裁掉本章 beat、禁止项和关键事实。每段保存 context_snapshot、prompt_version、model、参数与输入摘要/hash，以便复现。历史事实与未来计划分开传递，未来秘密只能作为“不可提前揭露”约束，不能作为人物已知事实。

### 4.3 章节生成流水线
确认章纲→建立 job（数据库唯一活动任务约束）→拆分 5 个段落计划（目标约 400—600 字，合计约 2500）→每段组包、调用流式 API、将增量事件推送 SSE 并持久化分段草稿→段末检查与续写→合并润色（不得新增情节）→字数检查（2300—2700，不足/超出仅针对内容修订）→一致性检查→写入章节待审草稿与摘要/变更草稿。人工编辑后必须对最新正文重新生成摘要及检查结果；用户点击“通过并入库”才在同一事务中确认摘要、角色状态变化、时间线和伏笔。下一章只读确认后的快照。

状态建议分离：`chapter_status = OUTLINED | GENERATING | REVIEW_PENDING | APPROVED`；`job_status = QUEUED | RUNNING | STOP_REQUESTED | STOPPED | FAILED | COMPLETED`；`audit_status = NOT_RUN | DRAFT | PASSED | NEEDS_REVIEW`。导出是操作记录而非不可逆章节状态。停止只取消未开始的后续段，保留已生成内容；继续从最后完整段或明确标记的未完成段恢复，避免重复追加。SSE 断线不等于任务停止，客户端用 Last-Event-ID 补读持久化事件。模型超时/限流采用有界重试和退避；对于已收到部分输出的调用，不盲目重试追加，先标记不完整再由用户确认恢复策略。

### 4.4 一致性与防幻觉
检查：角色/地名白名单、角色年龄/等级/能力、时间顺序、第三人称视角、章纲越界、后文概念提前揭露、伏笔状态。规则校验与模型自检结合；模型自检仅提出带正文引文的 `issue`，不能宣称保证零幻觉。新增设定形成待确认提案。所有摘要只依据当前已审核正文生成，不能把设定卡中尚未发生的事实写作“本章发生”。

## 5. 数据库设计（PostgreSQL）

主表及关键字段：

| 表 | 关键字段与用途 |
|---|---|
| users | id, email, password_hash, plan, created_at |
| novels | id, user_id, title, main_outline, status, version |
| volumes | id, novel_id, volume_no, title, events_jsonb, status |
| chapter_outlines | id, volume_id, chapter_no, title, beats_jsonb, characters_jsonb, status, version |
| chapters | id, novel_id, volume_id, outline_id, chapter_no, title, content_md, word_count, chapter_status, content_version, updated_at |
| chapter_versions | id, chapter_id, version_no, content_md, editor_type, created_at |
| setting_entities | id, novel_id, entity_type, name, aliases_jsonb, card_jsonb, source, approval_status, reveal_chapter |
| world_settings | id, novel_id, category, content, approval_status |
| timeline_events | id, novel_id, chapter_id, time_label, event, approval_status |
| foreshadowings | id, novel_id, set_chapter_id, payoff_chapter_id, content, status |
| summaries | id, novel_id, scope, ref_id, content, approval_status, source_content_version |
| style_profiles | id, novel_id, card_jsonb, sample_text |
| generation_jobs | id, chapter_id, status, current_segment, model, prompt_version, error_code, created_at, updated_at |
| generation_segments | id, job_id, segment_no, plan_jsonb, content, status, unique(job_id, segment_no) |
| generation_events | id, job_id, event_seq, event_type, payload_jsonb, unique(job_id, event_seq) |
| context_snapshots | id, job_id, segment_no, context_jsonb, prompt_hash, token_estimate |
| audit_reports | id, chapter_id, source_content_version, issues_jsonb, status |
| setting_change_drafts | id, chapter_id, source_content_version, changes_jsonb, status |
| usage_records | id, job_id, model, input_tokens, output_tokens, estimated_cost, created_at |
| audit_logs | id, user_id, action, target_type, target_id, created_at |

索引/约束：`volumes(novel_id,volume_no)`、`chapters(novel_id,chapter_no)` 唯一；所有外键及 `novels(user_id)` 索引；对章节采用乐观锁 `content_version`；PostgreSQL partial unique index 限制同一章 RUNNING/QUEUED 活动任务；所有确认写回在事务中执行，核对草稿 `source_content_version == chapters.content_version`，否则拒绝旧草稿入库。

## 6. Java 接口设计

统一响应：`{code,message,data,requestId}`；异步任务创建返回 `202 Accepted` + jobId。关键接口：

| 方法与路径 | 功能 |
|---|---|
| POST /api/auth/login | 登录 |
| POST /api/novels；GET /api/novels | 新建/列表 |
| POST /api/novels/{id}/outline/import | TXT 导入总纲（限制大小/编码校验） |
| PUT /api/novels/{id}/outline | 修改总纲 |
| POST /api/volumes/{id}/outline-generation | AI 拆本卷章纲 |
| PUT /api/volumes/{id}/outlines/confirm | 确认章纲 |
| GET/PUT /api/novels/{id}/settings | 读取/编辑设定 |
| POST /api/chapters/{id}/generation-jobs | 创建单章生成任务（Idempotency-Key） |
| GET /api/generation-jobs/{jobId}/events | SSE 事件流 |
| POST /api/generation-jobs/{jobId}/stop；POST .../resume | 停止/恢复 |
| PUT /api/chapters/{id}/content | 保存 Markdown，携带 expectedVersion |
| POST /api/chapters/{id}/review-draft | 按最新正文自检、生成摘要与设定变化草稿 |
| POST /api/chapters/{id}/approve | 人工确认，事务性入库 |
| GET /api/novels/{id}/export?format=docx | 导出保留卷/章标题 |
| GET /api/usage | 用量与成本统计 |

SSE 事件建议：`job.started`、`segment.started`、`text.delta`、`segment.completed`、`chapter.draft_ready`、`job.failed`、`job.stopped`、`job.completed`，均带单调递增 event id；禁止在事件中返回 API Key 或完整内部系统提示词。

核心 Java 接口示意：

```java
public interface ModelGateway {
    void stream(ModelRequest request, java.util.function.Consumer<ModelChunk> onChunk);
}
public interface ContextBuilder {
    ContextSnapshot build(long chapterId, int segmentNo);
}
public interface ChapterGenerationService {
    java.util.UUID start(long chapterId, String idempotencyKey);
    void requestStop(java.util.UUID jobId);
    void resume(java.util.UUID jobId);
}
```

## 7. 前端页面与交互

作品工作台：作品列表、总字数、已审核章节、用量；大纲页：总纲、卷事件、拆章纲与批量确认；设定库页：角色/世界/时间线/伏笔/文风及来源状态；创作页三栏：左侧章节树与章纲，中间 Markdown 正文及流式增量，右侧自检、摘要草稿与设定变更；审核页：正文、问题定位、可编辑的变更草稿、确认/退回。生成时锁定当前正文编辑或采用独立草稿区，避免 AI 流与用户输入相互覆盖。

## 8. 测试与验收

以《御兽：从一枚灰蛋开始》为独立测试 fixture：七卷总纲、角色卡、世界观、时间线、文风卡；第一卷约 18 章，前 12 章有期望章纲。第一章《废矿里的灰蛋》采用五段目标 500/550/450/450/550 字，核验衔接、无额外具名配角、不得提前解释“净息”“异兽皇”等、章末钩子、2300—2700 字、摘要不写入尚未出现的“左眼旧疤”。再用第十章《袭村》测试动作场景。测试类别：JUnit 5/Mockito 单测、Testcontainers PostgreSQL 集成测试、Mock AI 流式/断线/429/超时/半段失败、权限与并发、人工编辑后旧摘要拒绝入库、DOCX 标题层级及内容一致性、模型输出人工抽检。目标阈值建议：章纲格式校验 100%（失败重试/人工修复）、数据库状态不越级 100%、重复任务不产生重复正文；文学质量与幻觉率通过固定样本人工评分，不承诺模型绝对正确。

## 9. 安全、成本与运维

API Key 仅保存在服务端环境变量/密钥管理中；密码 BCrypt/Argon2；按作品鉴权；上传 TXT 校验编码、大小及文件类型；导出防路径穿越；日志脱敏，不记录完整用户小说正文；备份 PostgreSQL 与文件目录；用户内容默认不公开，是否被模型服务商留存以所选 API 条款为准。对每次模型调用记录实际 tokens、模型、估算费用；设月预算软阈值、单任务 token 上限和重生成次数上限。材料中 0.08—0.2 元/章等数字仅作为早期假设，需以真实计费实测更新。

## 10. 迭代实施计划

阶段一（基础）：搭建项目、登录、作品、三级大纲、数据库迁移与 TXT 导入。阶段二（创作闭环）：设定库、提示词版本、上下文组包、单章五段生成、SSE、停止恢复。阶段三（质量闭环）：Markdown 编辑/版本、自检、摘要/变更草稿、人工确认事务、下一章上下文。阶段四（交付）：DOCX 导出、成本统计、权限安全、自动化测试和 Docker 部署。首个纵向验收里程碑：用固定测试语料完整跑通第一章，从章纲确认直到审核入库，再生成第二章并证明不会提前泄露未来设定。

## 11. 主要风险与待决策事项

模型对结构化 JSON 的遵从率、中文“字数”统计口径、流式中断后如何处理不完整段落、不同厂商 token 计费与上下文窗口、AI 提案和人工事实冲突，均须在原型阶段验证。建议先选定一家支持流式与结构化输出的 API，做 10 章实测，再确定具体模型、超时、成本与段落字数策略。支付、审核合规和移动端属于后续详细设计，不应阻塞首版闭环。
