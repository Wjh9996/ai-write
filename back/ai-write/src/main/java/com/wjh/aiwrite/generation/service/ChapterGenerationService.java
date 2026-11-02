package com.wjh.aiwrite.generation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.config.AiProperties;
import com.wjh.aiwrite.generation.model.GenerationEvent;
import com.wjh.aiwrite.generation.model.GenerationJob;
import com.wjh.aiwrite.generation.model.GenerationSegment;
import com.wjh.aiwrite.generation.repository.GenerationEventRepository;
import com.wjh.aiwrite.generation.repository.GenerationJobRepository;
import com.wjh.aiwrite.generation.repository.GenerationSegmentRepository;
import com.wjh.aiwrite.outline.model.ChapterOutline;
import com.wjh.aiwrite.outline.repository.OutlineRepository;
import com.wjh.aiwrite.setting.model.SettingEntity;
import com.wjh.aiwrite.setting.model.StyleProfile;
import com.wjh.aiwrite.setting.repository.SettingEntityRepository;
import com.wjh.aiwrite.setting.repository.StyleProfileRepository;
import com.wjh.aiwrite.support.TextKit;
import com.wjh.aiwrite.usage.model.UsageRecord;
import com.wjh.aiwrite.usage.repository.UsageRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单章生成流水线（Model 层核心业务）
 *
 * 文档 §4.3 的状态机与规则在这里落地：
 *   - 确认章纲才允许生成（规则①）
 *   - 一章只允许一个活动任务（规则⑦，数据库生成列兜底）
 *   - 停止只取消未开始的后续段，保留已生成内容
 *   - 每段保存 context_snapshot 以便复现
 *   - 生成完成只写入「待审草稿」，不自动入库（规则③）
 */
@Slf4j
@Service
public class ChapterGenerationService {

    private final GenerationJobRepository jobRepository;
    private final OutlineRepository outlineRepository;
    private final ChapterRepository chapterRepository;
    private final SettingEntityRepository entityRepository;
    private final GenerationSegmentRepository segmentRepository;
    private final GenerationEventRepository eventRepository;
    private final StyleProfileRepository styleRepository;
    private final UsageRecordRepository usageRepository;
    private final ModelGateway modelGateway;
    private final SseHub sseHub;
    private final AiProperties props;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 生成任务跑在独立线程池，不占用 Tomcat 请求线程 */
    private final Executor executor;

    /** 事件落库专用单线程，避免阻塞生成循环 */
    private final Executor eventWriter;
    private final AtomicInteger pendingWrites = new AtomicInteger(0);

    private final Map<String, AtomicLong> seqCounters = new ConcurrentHashMap<>();
    private final Map<String, Boolean> stopFlags = new ConcurrentHashMap<>();

    public ChapterGenerationService(GenerationJobRepository jobRepository,
                                    OutlineRepository outlineRepository,
                                    ChapterRepository chapterRepository,
                                    SettingEntityRepository entityRepository,
                                    GenerationSegmentRepository segmentRepository,
                                    GenerationEventRepository eventRepository,
                                    StyleProfileRepository styleRepository,
                                    UsageRecordRepository usageRepository,
                                    ModelGateway modelGateway,
                                    SseHub sseHub,
                                    AiProperties props,
                                    @Qualifier("generationExecutor") Executor executor,
                                    @Qualifier("eventWriterExecutor") Executor eventWriter) {
        this.jobRepository = jobRepository;
        this.outlineRepository = outlineRepository;
        this.chapterRepository = chapterRepository;
        this.entityRepository = entityRepository;
        this.segmentRepository = segmentRepository;
        this.eventRepository = eventRepository;
        this.styleRepository = styleRepository;
        this.usageRepository = usageRepository;
        this.modelGateway = modelGateway;
        this.sseHub = sseHub;
        this.props = props;
        this.executor = executor;
        this.eventWriter = eventWriter;
    }

    // ---------------- 任务生命周期 ----------------

    /**
     * 创建任务：返回 202 Accepted 语义的 jobId
     *
     * 并发控制不靠事务：generation_jobs 的 uk_active_chapter（生成列 + 唯一索引）
     * 会在数据库层面拦住「同章第二个活动任务」，比应用层判断更可靠。
     */
    public GenerationJob start(Long chapterId, String idempotencyKey) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("章节不存在"));

        // 幂等：同样的 key 直接返回已有任务
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<GenerationJob> dup = jobRepository.findByIdempotencyKey(idempotencyKey);
            if (dup.isPresent()) {
                return dup.get();
            }
        }

        // 规则①：章纲必须已确认
        if (chapter.getOutlineId() != null) {
            ChapterOutline outline = outlineRepository.findById(chapter.getOutlineId()).orElse(null);
            if (outline != null && !"CONFIRMED".equals(outline.getStatus())) {
                throw new IllegalStateException("本章章纲尚未确认，不能生成");
            }
        }
        // 规则⑦：一章一个活动任务（数据库唯一索引还会再兜一次）
        if (!jobRepository.findActiveByChapter(chapterId).isEmpty()) {
            throw new IllegalStateException("本章已有进行中的生成任务");
        }
        // 规则②：依赖上一章必须已审核
        if (chapter.getChapterNo() > 1) {
            chapterRepository.findByNovelIdOrderByChapterNo(chapter.getNovelId()).stream()
                    .filter(c -> c.getChapterNo() == chapter.getChapterNo() - 1)
                    .findFirst()
                    .ifPresent(prev -> {
                        if (!"APPROVED".equals(prev.getChapterStatus())) {
                            throw new IllegalStateException("前一章尚未通过审核，不能继续创作");
                        }
                    });
        }

        GenerationJob job = new GenerationJob();
        job.setId(UUID.randomUUID().toString());
        job.setChapterId(chapterId);
        job.setStatus("QUEUED");
        job.setCurrentSegment(0);
        job.setTotalSegments(segmentTargets().size());
        job.setModel(props == null ? "" : String.valueOf(props.model()));
        job.setPromptVersion("v1.3");
        job.setIdempotencyKey(idempotencyKey);
        GenerationJob saved = jobRepository.save(job);

        chapter.setChapterStatus("GENERATING");
        chapterRepository.save(chapter);

        seqCounters.put(saved.getId(), new AtomicLong(0));
        stopFlags.remove(saved.getId());
        return saved;
    }

    /** 请求停止：只影响后续段 */
    public void requestStop(String jobId) {
        stopFlags.put(jobId, Boolean.TRUE);
        jobRepository.findById(jobId).ifPresent(j -> {
            if ("RUNNING".equals(j.getStatus()) || "QUEUED".equals(j.getStatus())) {
                j.setStatus("STOP_REQUESTED");
                jobRepository.save(j);
            }
        });
    }

    /** 从最后一个完整段继续 */
    public void resume(String jobId) {
        stopFlags.remove(jobId);
        jobRepository.findById(jobId).ifPresent(j -> {
            j.setStatus("QUEUED");
            jobRepository.save(j);
        });
        run(jobId);
    }

    /**
     * 提交到独立线程池执行，避免占用 Tomcat 线程。
     *
     * 启动前先等一小会儿，让客户端把 SSE 连接建好：
     * 否则 job.started / 前几个 delta 会在连接建立之前就发完，客户端看不到开头。
     * 等待最多 2.5 秒，超时照样开跑（后端不能依赖前端一定连上来）。
     */
    public void runAsync(String jobId) {
        executor.execute(() -> {
            sseHub.awaitFirstConnection(jobId, 2500);
            run(jobId);
        });
    }

    // ---------------- 流水线 ----------------

    /**
     * 流水线主流程。
     * 刻意不加类级事务：整个生成可能持续十几秒，长事务会一直占着数据库连接；
     * 这里每一步单独 save，由 JPA 各自开短事务，保证事件能即时落库供断线补读。
     */
    protected void run(String jobId) {
        GenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        if ("COMPLETED".equals(job.getStatus())) {
            return;
        }

        Chapter chapter = chapterRepository.findById(job.getChapterId())
                .orElseThrow(() -> new IllegalArgumentException("章节不存在"));

        job.setStatus("RUNNING");
        jobRepository.save(job);
        emit(job, "job.started", Map.of(
                "jobId", jobId,
                "chapterId", job.getChapterId(),
                "totalSegments", job.getTotalSegments()));

        List<Integer> targets = segmentTargets();
        int from = resumePoint(job);
        StringBuilder whole = new StringBuilder(baseContent(job, chapter));

        for (int i = from; i <= targets.size(); i++) {
            if (Boolean.TRUE.equals(stopFlags.get(jobId))) {
                break;
            }
            int segNo = i;
            int target = targets.get(i - 1);

            job.setCurrentSegment(segNo);
            jobRepository.save(job);

            emit(job, "segment.started", Map.of(
                    "segment", Map.of(
                            "segmentNo", segNo,
                            "targetWords", target,
                            "written", 0,
                            "status", "RUNNING")));

            String prompt = renderPrompt(chapter, segNo, targets.size(), target, whole.toString());
            StringBuilder buf = new StringBuilder();
            modelGateway.stream(
                    new ModelGateway.ModelRequest(systemPrompt(chapter), prompt, target, props.model()),
                    chunk -> {
                        if (chunk.delta() == null || chunk.delta().isEmpty()) return;
                        buf.append(chunk.delta());
                        emit(job, "text.delta", Map.of(
                                "segmentNo", segNo,
                                "text", chunk.delta()));
                    });

            String text = buf.toString();
            whole.append(whole.length() == 0 ? "" : "\n\n").append(text);

            // 持久化分段（便于停止后恢复）
            var seg = segmentRepository.findByJobIdAndSegmentNo(jobId, segNo)
                    .orElseGet(() -> {
                        GenerationSegment s = new GenerationSegment();
                        s.setJobId(jobId);
                        s.setSegmentNo(segNo);
                        return s;
                    });
            seg.setContent(text);
            seg.setStatus("COMPLETED");
            seg.setPlanJson("{\"targetWords\":" + target + "}");
            segmentRepository.save(seg);

            emit(job, "segment.completed", Map.of(
                    "segmentNo", segNo,
                    "written", TextKit.countWords(text)));

            recordUsage(jobId, "分段生成", estimateTokens(prompt), estimateTokens(text));
        }

        boolean stopped = Boolean.TRUE.equals(stopFlags.get(jobId));
        if (stopped) {
            job.setStatus("STOPPED");
            jobRepository.save(job);
            chapter.setChapterStatus("REVIEW_PENDING");
            chapter.setContentMd(whole.toString());
            chapter.setWordCount(TextKit.countWords(whole.toString()));
            chapterRepository.save(chapter);
            emit(job, "job.stopped", Map.of("jobId", jobId, "atSegment", job.getCurrentSegment()));
        } else {
            String content = whole.toString();
            chapter.setContentMd(content);
            chapter.setWordCount(TextKit.countWords(content));
            chapter.setChapterStatus("REVIEW_PENDING");
            chapter.setAuditStatus("NOT_RUN");
            chapterRepository.save(chapter);

            job.setStatus("COMPLETED");
            jobRepository.save(job);
            emit(job, "chapter.draft_ready", Map.of(
                    "content", content,
                    "wordCount", chapter.getWordCount()));
            emit(job, "job.completed", Map.of("jobId", jobId));
        }
        awaitWrites();
        sseHub.complete(jobId);
        seqCounters.remove(jobId);
        stopFlags.remove(jobId);
    }

    // ---------------- 提示词组包（文档 §4.2） ----------------

    private String systemPrompt(Chapter chapter) {
        StyleProfile style = styleRepository.findByNovelId(chapter.getNovelId()).orElse(null);
        StringBuilder sb = new StringBuilder("你是网文作者，用第三人称、正能量、有深度的风格续写。\n");
        if (style != null && style.getCard() != null && !style.getCard().isEmpty()) {
            sb.append("【文风卡】\n");
            style.getCard().forEach((k, v) -> sb.append(k).append("：").append(v).append("\n"));
        }
        sb.append("""
                禁止：新增设定库外关键角色/地名；改变人称；让角色知道未发生的信息；写敏感内容。
                """);
        return sb.toString();
    }

    private String renderPrompt(Chapter chapter, int segNo, int total, int targetWords, String lastText) {
        ChapterOutline outline = chapter.getOutlineId() == null ? null
                : outlineRepository.findById(chapter.getOutlineId()).orElse(null);
        List<SettingEntity> entities = entityRepository.findByNovelId(chapter.getNovelId());

        String beats = outline == null ? "（无章纲）"
                : String.join("\n", outline.getBeats());
        String tail = lastText.length() > 200 ? lastText.substring(lastText.length() - 200) : lastText;

        return """
                你是网文作者，用第三人称、正能量、有深度的风格续写。
                【本章章纲】%s
                【相关角色卡】%s
                【世界观/时间线/伏笔】见已确认设定摘要
                任务：写第 %d/%d 段，%d 字左右，承接上文，只推进本段对应 beat，结尾留自然钩子。
                【上一段结尾】%s
                """.formatted(
                beats,
                entities.stream().map(SettingEntity::getName).reduce((a, b) -> a + "、" + b).orElse("无"),
                segNo, total, targetWords, tail);
    }

    // ---------------- 事件与用量 ----------------

    /**
     * 发出事件：同步推送给在线连接（保证实时性），异步落库（保证不拖慢生成）。
     *
     * 为什么落库要异步：一次生成会产生数百条 text.delta，
     * 每条都同步 save 会把十几秒的生成拖到几十秒 —— 事件落库只是为了断线补读，
     * 晚几百毫秒没有影响，但必须在任务结束时确保全部写完（见 finishPendingWrites）。
     *
     * 注意：payload 中不含 API Key，也不含完整系统提示词（文档 §6）。
     */
    private void emit(GenerationJob job, String type, Map<String, Object> payload) {
        long seq = seqCounters.computeIfAbsent(job.getId(), k -> new AtomicLong(0)).incrementAndGet();

        GenerationEvent e = new GenerationEvent();
        e.setJobId(job.getId());
        e.setEventSeq(seq);
        e.setEventType(type);
        String json;
        try {
            json = mapper.writeValueAsString(payload);
        } catch (Exception ex) {
            json = "{}";
        }
        e.setPayloadJson(json);

        pendingWrites.incrementAndGet();
        eventWriter.execute(() -> {
            try {
                eventRepository.save(e);
            } catch (Exception ex) {
                log.warn("[gen] 事件落库失败 seq={} {}", seq, ex.getMessage());
            } finally {
                pendingWrites.decrementAndGet();
            }
        });

        sseHub.send(job.getId(), Map.of(
                "seq", seq,
                "type", type,
                "payload", payload), seq);
    }

    /** 任务收尾时等落库写完，避免客户端补读时缺尾部事件 */
    private void awaitWrites() {
        long deadline = System.currentTimeMillis() + 5000;
        while (pendingWrites.get() > 0 && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(30);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void recordUsage(String jobId, String scene, int inTokens, int outTokens) {
        UsageRecord r = new UsageRecord();
        r.setJobId(jobId);
        r.setScene(scene);
        r.setModel(props == null ? "" : String.valueOf(props.model()));
        r.setInputTokens(inTokens);
        r.setOutputTokens(outTokens);
        r.setEstimatedCost(cost(inTokens, outTokens));
        usageRepository.save(r);
    }

    private BigDecimal cost(int inTokens, int outTokens) {
        double ip = props == null || props.cost() == null || props.cost().inputPricePerK() == null
                ? 0.002 : props.cost().inputPricePerK();
        double op = props == null || props.cost() == null || props.cost().outputPricePerK() == null
                ? 0.008 : props.cost().outputPricePerK();
        double c = inTokens / 1000.0 * ip + outTokens / 1000.0 * op;
        return BigDecimal.valueOf(c).setScale(4, RoundingMode.HALF_UP);
    }

    // ---------------- 辅助 ----------------

    private List<Integer> segmentTargets() {
        return props == null || props.generation() == null
                ? List.of(500, 550, 450, 450, 550)
                : props.generation().segmentTargetList();
    }

    /** 从最后一个完整段的下一号继续 */
    private int resumePoint(GenerationJob job) {
        List<GenerationSegment> segs = segmentRepository.findByJobIdOrderBySegmentNo(job.getId());
        return segs.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .mapToInt(GenerationSegment::getSegmentNo)
                .max()
                .orElse(0) + 1;
    }

    /** 已生成的内容（停止后继续时作为前缀） */
    private String baseContent(GenerationJob job, Chapter chapter) {
        List<GenerationSegment> segs = segmentRepository.findByJobIdOrderBySegmentNo(job.getId());
        String joined = segs.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .map(GenerationSegment::getContent)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + "\n\n" + b).orElse("");
        return joined.isBlank() ? (chapter.getContentMd() == null ? "" : chapter.getContentMd()) : joined;
    }

    /** 粗略 token 估算：中文约 1.5 token/字 */
    private int estimateTokens(String text) {
        return (int) (TextKit.countWords(text) * 1.5);
    }
}
