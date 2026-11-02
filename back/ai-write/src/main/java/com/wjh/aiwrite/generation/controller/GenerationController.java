package com.wjh.aiwrite.generation.controller;

import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.generation.dto.CreateJobResult;
import com.wjh.aiwrite.generation.dto.JobStatusDto;
import com.wjh.aiwrite.generation.model.GenerationEvent;
import com.wjh.aiwrite.generation.model.GenerationJob;
import com.wjh.aiwrite.generation.repository.GenerationEventRepository;
import com.wjh.aiwrite.generation.repository.GenerationJobRepository;
import com.wjh.aiwrite.generation.service.ChapterGenerationService;
import com.wjh.aiwrite.generation.service.SseHub;
import com.wjh.aiwrite.support.Ownership;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 生成 Controller（View 层）—— 文档 §6
 *
 * POST /api/chapters/{id}/generation-jobs  创建任务（202 Accepted + jobId）
 * POST /api/generation-jobs/{id}/stop      停止
 * POST /api/generation-jobs/{id}/resume    继续
 * GET  /api/generation-jobs/{id}/events    SSE 事件流（支持 Last-Event-ID 补读）
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GenerationController {

    private final ChapterGenerationService generationService;
    private final GenerationJobRepository jobRepository;
    private final GenerationEventRepository eventRepository;
    private final ChapterRepository chapterRepository;
    private final SseHub sseHub;
    private final Ownership ownership;

    /** 创建单章生成任务，返回 202 + jobId */
    @PostMapping("/chapters/{chapterId}/generation-jobs")
    public ResponseEntity<ApiResult<CreateJobResult>> start(
            @PathVariable Long chapterId,
            @RequestBody(required = false) Map<String, String> body) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("章节不存在"));
        ownership.requireNovel(chapter.getNovelId());

        String key = body == null ? null : body.get("idempotencyKey");
        GenerationJob job = generationService.start(chapterId, key);

        // 异步启动流水线，立即返回 202
        generationService.runAsync(job.getId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResult.ok(new CreateJobResult(
                        job.getId(), job.getStatus(), job.getCurrentSegment(), job.getTotalSegments())));
    }

    @PostMapping("/generation-jobs/{jobId}/stop")
    public ApiResult<Void> stop(@PathVariable String jobId) {
        requireJob(jobId);
        generationService.requestStop(jobId);
        return ApiResult.ok(null, "已请求停止");
    }

    @PostMapping("/generation-jobs/{jobId}/resume")
    public ApiResult<Void> resume(@PathVariable String jobId) {
        requireJob(jobId);
        generationService.resume(jobId);
        return ApiResult.ok(null, "已继续生成");
    }

    @GetMapping("/generation-jobs/{jobId}")
    public ApiResult<JobStatusDto> status(@PathVariable String jobId) {
        GenerationJob job = requireJob(jobId);
        return ApiResult.ok(new JobStatusDto(
                job.getId(),
                job.getChapterId(),
                job.getStatus(),
                job.getCurrentSegment(),
                job.getTotalSegments(),
                job.getModel(),
                job.getPromptVersion(),
                job.getErrorCode(),
                String.valueOf(job.getCreatedAt()),
                String.valueOf(job.getUpdatedAt())));
    }

    /**
     * SSE 事件流。
     * 文档 §4.3：断线不等于任务停止 —— 重连带 Last-Event-ID 时，
     * 先把该序号之后的持久化事件补发，再接上实时流。
     */
    @GetMapping(value = "/generation-jobs/{jobId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@PathVariable String jobId,
                             @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        requireJob(jobId);
        SseEmitter emitter = sseHub.create(jobId);

        long from = parseSeq(lastEventId);
        if (from > 0) {
            List<GenerationEvent> missed = eventRepository
                    .findByJobIdAndEventSeqGreaterThanOrderByEventSeq(jobId, from);
            missed.forEach(e -> sseHub.send(jobId, toFrame(e), e.getEventSeq()));
        }
        return emitter;
    }

    // ---------------- 辅助 ----------------

    private GenerationJob requireJob(String jobId) {
        GenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
        Chapter chapter = chapterRepository.findById(job.getChapterId())
                .orElseThrow(() -> new IllegalArgumentException("章节不存在"));
        ownership.requireNovel(chapter.getNovelId());
        return job;
    }

    private static long parseSeq(String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(lastEventId.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 把库里的事件还原成 SSE 帧体（与实时推送的帧格式一致） */
    private static Map<String, Object> toFrame(GenerationEvent e) {
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("seq", e.getEventSeq());
        frame.put("type", e.getEventType());
        frame.put("payload", e.getPayloadJson());
        return frame;
    }
}
