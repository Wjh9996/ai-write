package com.wjh.aiwrite.outline;

import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.outline.dto.OutlineDto;
import com.wjh.aiwrite.outline.repository.OutlineRepository;
import com.wjh.aiwrite.outline.repository.VolumeRepository;
import com.wjh.aiwrite.outline.service.OutlineService;
import com.wjh.aiwrite.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 大纲服务测试
 *
 * 覆盖：章纲 beat 数硬校验（3—7 条）、确认后生成章节（规则①）、
 * 重新生成时保留已确认章纲。
 */
@SpringBootTest
@ActiveProfiles("h2")
class OutlineServiceTest {

    @Autowired
    private OutlineService outlineService;
    @Autowired
    private OutlineRepository outlineRepository;
    @Autowired
    private VolumeRepository volumeRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private TestFixtures fixtures;

    @Test
    @DisplayName("beat 少于 3 条：拒绝保存（文档 §4.1）")
    void tooFewBeatsRejected() {
        var f = fixtures.full();
        var patch = new OutlineDto(f.outline().getId(), f.volume().getId(), 1,
                "破壳之前", List.of("只有一条"), List.of("林昭"), 2500, "CONFIRMED", 1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> outlineService.update(f.outline().getId(), patch));
        assertTrue(ex.getMessage().contains("3—7"));
    }

    @Test
    @DisplayName("beat 多于 7 条：拒绝保存")
    void tooManyBeatsRejected() {
        var f = fixtures.full();
        var patch = new OutlineDto(f.outline().getId(), f.volume().getId(), 1,
                "破壳之前",
                List.of("1", "2", "3", "4", "5", "6", "7", "8"),
                List.of("林昭"), 2500, "CONFIRMED", 1);

        assertThrows(IllegalArgumentException.class,
                () -> outlineService.update(f.outline().getId(), patch));
    }

    @Test
    @DisplayName("确认本卷：章纲转 CONFIRMED，并生成 OUTLINED 章节")
    void confirmCreatesChapters() {
        var f = fixtures.full();

        var vol = outlineService.confirm(f.volume().getId());

        assertEquals("CONFIRMED", vol.status());
        assertTrue(outlineRepository.findByVolumeIdOrderByChapterNo(f.volume().getId())
                .stream().allMatch(o -> "CONFIRMED".equals(o.getStatus())));

        var chapters = chapterRepository.findByNovelIdOrderByChapterNo(f.novel().getId());
        assertFalse(chapters.isEmpty());
        assertTrue(chapters.stream().allMatch(c -> "OUTLINED".equals(c.getChapterStatus())),
                "确认章纲后章节进入 OUTLINED，而不是直接可生成正文");
    }

    @Test
    @DisplayName("AI 拆章纲：只覆盖草稿，已确认的章纲保留")
    void generateKeepsConfirmed() {
        var f = fixtures.full();

        var result = outlineService.generate(f.volume().getId());

        assertFalse(result.outlines().isEmpty());
        assertTrue(result.proposedEntities().size() > 0, "新实体应作为「待新增设定」返回");
        assertTrue(result.outlines().stream()
                        .filter(o -> o.title().contains("破壳之前"))
                        .findFirst()
                        .isPresent(),
                "第 1 章已确认，重新生成后必须还在");
        assertTrue(result.outlines().stream().allMatch(
                o -> o.beats().size() >= 3 && o.beats().size() <= 7));
    }

    @Test
    @DisplayName("插入章纲：后续章号顺延")
    void insertShiftsChapterNo() {
        var f = fixtures.full();
        fixtures.newOutline(f.volume(), 2, "孵化", List.of("第一次失败", "第二次失败", "章末钩子"));
        var inserted = outlineService.insert(f.volume().getId(), 1);

        assertEquals(2, inserted.chapterNo());
        var all = outlineRepository.findByVolumeIdOrderByChapterNo(f.volume().getId());
        assertEquals(3, all.size(), "共 3 条：原第 1、插入的新第 2、顺延后的第 3");
        assertTrue(all.stream().anyMatch(o -> o.getChapterNo() == 3), "原第 2 条应顺延到 3");
    }
}
