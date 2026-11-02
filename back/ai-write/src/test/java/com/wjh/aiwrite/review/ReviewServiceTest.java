package com.wjh.aiwrite.review;

import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.common.BizException;
import com.wjh.aiwrite.review.dto.ApproveRequest;
import com.wjh.aiwrite.review.dto.ChangeDto;
import com.wjh.aiwrite.review.dto.ReviewDraftDto;
import com.wjh.aiwrite.review.service.ReviewService;
import com.wjh.aiwrite.setting.repository.SettingEntityRepository;
import com.wjh.aiwrite.setting.repository.TimelineEventRepository;
import com.wjh.aiwrite.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审核服务测试
 *
 * 覆盖不可妥协规则③：只有人工确认后，摘要 / 角色状态 / 时间线 / 伏笔才写回。
 */
@SpringBootTest
@ActiveProfiles("h2")
class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private SettingEntityRepository entityRepository;
    @Autowired
    private TimelineEventRepository timelineRepository;
    @Autowired
    private TestFixtures fixtures;

    private Chapter chapterWithContent() {
        var f = fixtures.full();
        Chapter c = chapterRepository.findById(f.chapter().getId()).orElseThrow();
        c.setContentMd("矿洞里的水一直在滴。林昭把蛋揣进怀里。灰羽破壳了。");
        c.setWordCount(24);
        c.setChapterStatus("REVIEW_PENDING");
        return chapterRepository.save(c);
    }

    @Test
    @DisplayName("没有正文时自检应被拒绝")
    void draftWithoutContentRejected() {
        var f = fixtures.full();
        assertThrows(BizException.class, () -> reviewService.draft(f.chapter().getId()));
    }

    @Test
    @DisplayName("自检生成：问题 + 摘要 + 变更草稿")
    void draftProducesAll() {
        Chapter c = chapterWithContent();

        ReviewDraftDto draft = reviewService.draft(c.getId());

        assertNotNull(draft.issues());
        assertNotNull(draft.summary().content());
        assertFalse(draft.summary().content().isBlank());
        assertFalse(draft.changes().isEmpty(), "应给出时间线等变更提案");
        assertEquals(c.getContentVersion(), draft.summary().sourceContentVersion(),
                "摘要必须标注它基于哪个正文版本生成（规则③）");
    }

    @Test
    @DisplayName("通过入库：章节转 APPROVED，接受的变更写回时间线")
    void approveWritesBack() {
        Chapter c = chapterWithContent();
        ReviewDraftDto draft = reviewService.draft(c.getId());

        long timelineBefore = timelineRepository.count();

        reviewService.approve(c.getId(), new ApproveRequest(
                draft.summary().content(),
                draft.changes().stream()
                        .map(ch -> new ChangeDto(ch.id(), ch.kind(), ch.name(), ch.change(),
                                ch.time(), ch.action(), ch.entityType(), true))
                        .toList(),
                null));

        Chapter after = chapterRepository.findById(c.getId()).orElseThrow();
        assertEquals("APPROVED", after.getChapterStatus());
        assertEquals("PASSED", after.getAuditStatus());
        assertTrue(timelineRepository.count() > timelineBefore, "接受的时间线变更应已落库");
    }

    @Test
    @DisplayName("未接受的变更不写回（人工可逐条勾选）")
    void unacceptedChangesIgnored() {
        Chapter c = chapterWithContent();
        ReviewDraftDto draft = reviewService.draft(c.getId());
        long timelineBefore = timelineRepository.count();

        reviewService.approve(c.getId(), new ApproveRequest(
                draft.summary().content(),
                draft.changes().stream()
                        .map(ch -> new ChangeDto(ch.id(), ch.kind(), ch.name(), ch.change(),
                                ch.time(), ch.action(), ch.entityType(), false))
                        .toList(),
                null));

        assertEquals(timelineBefore, timelineRepository.count(), "全部不勾就不应写任何东西");
        assertEquals("APPROVED",
                chapterRepository.findById(c.getId()).orElseThrow().getChapterStatus());
    }

    @Test
    @DisplayName("摘要为空：拒绝入库")
    void emptySummaryRejected() {
        Chapter c = chapterWithContent();
        ReviewDraftDto draft = reviewService.draft(c.getId());

        assertThrows(BizException.class, () -> reviewService.approve(c.getId(),
                new ApproveRequest("  ", draft.changes(), null)));
    }

    @Test
    @DisplayName("新增设定类变更：按 entityType 登记进设定库")
    void newEntityCreated() {
        Chapter c = chapterWithContent();
        long entityBefore = entityRepository.count();

        reviewService.approve(c.getId(), new ApproveRequest("本章摘要", List.of(
                new ChangeDto("c-new", "NEW_ENTITY", "岩甲蟹", "野外常见一阶异兽，弱点是腹甲接缝",
                        null, null, "CHARACTER", true)
        ), null));

        assertEquals(entityBefore + 1, entityRepository.count());
        assertTrue(entityRepository.findAll().stream()
                .anyMatch(e -> "岩甲蟹".equals(e.getName())));
    }

    @Test
    @DisplayName("退回修改：回到 GENERATING / NEEDS_REVIEW")
    void rejectRollsBack() {
        Chapter c = chapterWithContent();
        reviewService.reject(c.getId(), "第一章不能提净息符");

        Chapter after = chapterRepository.findById(c.getId()).orElseThrow();
        assertEquals("GENERATING", after.getChapterStatus());
        assertEquals("NEEDS_REVIEW", after.getAuditStatus());
    }
}
