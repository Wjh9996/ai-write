package com.wjh.aiwrite.generation;

import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.generation.model.GenerationEvent;
import com.wjh.aiwrite.generation.model.GenerationJob;
import com.wjh.aiwrite.generation.model.GenerationSegment;
import com.wjh.aiwrite.generation.repository.GenerationEventRepository;
import com.wjh.aiwrite.generation.repository.GenerationJobRepository;
import com.wjh.aiwrite.generation.repository.GenerationSegmentRepository;
import com.wjh.aiwrite.generation.service.ChapterGenerationService;
import com.wjh.aiwrite.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 生成流水线测试（文档 §4.3 的状态机与规则）
 *
 * 说明：mock 网关用本地语料，单章生成约十秒，这里的断言等待上限设为 60 秒。
 */
@SpringBootTest
@ActiveProfiles("h2")
class GenerationPipelineTest {

    @Autowired
    private ChapterGenerationService generationService;
    @Autowired
    private GenerationJobRepository jobRepository;
    @Autowired
    private GenerationSegmentRepository segmentRepository;
    @Autowired
    private GenerationEventRepository eventRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private com.wjh.aiwrite.outline.repository.OutlineRepository outlineRepository;
    @Autowired
    private TestFixtures fixtures;

    /** 等任务跑到终态 */
    private GenerationJob awaitDone(String jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            var job = jobRepository.findById(jobId).orElseThrow();
            if ("COMPLETED".equals(job.getStatus()) || "STOPPED".equals(job.getStatus())
                    || "FAILED".equals(job.getStatus())) {
                return job;
            }
            Thread.sleep(200);
        }
        fail("生成任务超时未结束");
        return null;
    }

    @Test
    @DisplayName("完整跑完：五段生成 → 事件落库 → 章节转待审，字数在验收区间")
    void fullPipeline() throws Exception {
        var f = fixtures.full();
        GenerationJob job = generationService.start(f.chapter().getId(), "key-" + System.nanoTime());
        assertEquals("QUEUED", job.getStatus());

        generationService.runAsync(job.getId());
        GenerationJob done = awaitDone(job.getId());

        assertEquals("COMPLETED", done.getStatus());
        assertEquals(5, done.getTotalSegments(), "默认五段");

        // 分段落库
        List<GenerationSegment> segs = segmentRepository.findByJobIdOrderBySegmentNo(job.getId());
        assertEquals(5, segs.size(), "应留下 5 段草稿");
        assertTrue(segs.stream().allMatch(s -> "COMPLETED".equals(s.getStatus())));

        // 事件落库且 seq 单调
        List<GenerationEvent> events = eventRepository
                .findByJobIdAndEventSeqGreaterThanOrderByEventSeq(job.getId(), 0L);
        assertFalse(events.isEmpty(), "事件必须落库，否则断线无法补读");
        for (int i = 1; i < events.size(); i++) {
            assertTrue(events.get(i).getEventSeq() > events.get(i - 1).getEventSeq(),
                    "event_seq 必须单调递增（Last-Event-ID 补读依赖它）");
        }
        assertTrue(events.stream().anyMatch(e -> "job.started".equals(e.getEventType())));
        assertTrue(events.stream().anyMatch(e -> "text.delta".equals(e.getEventType())));
        assertTrue(events.stream().anyMatch(e -> "job.completed".equals(e.getEventType())));

        // 章节状态与字数
        Chapter after = chapterRepository.findById(f.chapter().getId()).orElseThrow();
        assertEquals("REVIEW_PENDING", after.getChapterStatus(),
                "生成完只进入待审，不能自动入库（规则③）");
        assertTrue(after.getWordCount() >= 2300 && after.getWordCount() <= 2700,
                "字数应落在 2300—2700，实际 " + after.getWordCount());
        assertFalse(after.getContentMd().isBlank());
    }

    @Test
    @DisplayName("幂等：同一 key 重复创建返回同一任务，不产生重复正文")
    void idempotency() {
        var f = fixtures.full();
        String key = "idem-" + System.nanoTime();

        GenerationJob first = generationService.start(f.chapter().getId(), key);
        GenerationJob second = generationService.start(f.chapter().getId(), key);

        assertEquals(first.getId(), second.getId(), "重复提交必须命中同一任务");
        assertEquals(1, jobRepository.findActiveByChapter(f.chapter().getId()).size());
    }

    @Test
    @DisplayName("规则⑦：一章只允许一个活动任务")
    void onlyOneActiveJobPerChapter() {
        var f = fixtures.full();

        generationService.start(f.chapter().getId(), "k1-" + System.nanoTime());
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> generationService.start(f.chapter().getId(), "k2-" + System.nanoTime()));

        assertTrue(ex.getMessage().contains("进行中"));
        // 数据库层还有 uk_active_chapter（MySQL 生成列）兜底，见 schema.sql
    }

    @Test
    @DisplayName("规则①：章纲未确认不得生成")
    void unconfirmedOutlineBlocked() {
        var f = fixtures.full();

        // 把第 1 章对应的章纲改回草稿状态
        var outline = outlineRepository.findById(f.outline().getId()).orElseThrow();
        outline.setStatus("DRAFT");
        outlineRepository.save(outline);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> generationService.start(f.chapter().getId(), "k-" + System.nanoTime()));

        assertTrue(ex.getMessage().contains("章纲尚未确认"),
                "未确认章纲绝不能起生成任务");
    }

    @Test
    @DisplayName("停止：任务转 STOPPED，已生成内容保留")
    void stopKeepsPartialContent() throws Exception {
        var f = fixtures.full();
        GenerationJob job = generationService.start(f.chapter().getId(), "stop-" + System.nanoTime());

        generationService.runAsync(job.getId());
        // 至少要等第一段写完再停：runAsync 会先等 2.5s 让 SSE 连接建立
        Thread.sleep(5000);
        generationService.requestStop(job.getId());

        GenerationJob done = awaitDone(job.getId());
        assertEquals("STOPPED", done.getStatus());

        Chapter after = chapterRepository.findById(f.chapter().getId()).orElseThrow();
        assertFalse(after.getContentMd() == null || after.getContentMd().isBlank(),
                "停止只取消未开始的后续段，已生成的内容必须保留（文档 §4.3）");
        assertTrue(segmentRepository.findByJobIdOrderBySegmentNo(job.getId()).size() >= 1);
    }
}
