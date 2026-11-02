package com.wjh.aiwrite.chapter;

import com.wjh.aiwrite.chapter.dto.ChapterDto;
import com.wjh.aiwrite.chapter.dto.SaveContentRequest;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.chapter.service.ChapterService;
import com.wjh.aiwrite.common.BizException;
import com.wjh.aiwrite.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 章节服务测试
 *
 * 覆盖不可妥协规则⑤：AI 不得直接覆盖人工编辑 —— 用 content_version 乐观锁保证。
 */
@SpringBootTest
@ActiveProfiles("h2")
class ChapterServiceTest {

    @Autowired
    private ChapterService chapterService;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private TestFixtures fixtures;

    @Test
    @DisplayName("保存正文：版本号递增，字数按中文口径统计")
    void saveContentIncrementsVersion() {
        var f = fixtures.full();
        int v0 = f.chapter().getContentVersion();

        ChapterDto saved = chapterService.saveContent(f.chapter().getId(),
                new SaveContentRequest("矿洞里的水一直在滴。林昭数到第三百二十七滴。", v0));

        assertEquals(v0 + 1, saved.contentVersion(), "保存后版本号应 +1");
        assertEquals(20, saved.wordCount(), "字数统计应排除标点");
    }

    @Test
    @DisplayName("旧版本号保存被拒：409 冲突（规则⑤）")
    void staleVersionRejected() {
        var f = fixtures.full();
        int v0 = f.chapter().getContentVersion();

        chapterService.saveContent(f.chapter().getId(), new SaveContentRequest("第一版正文。", v0));

        BizException ex = assertThrows(BizException.class, () ->
                chapterService.saveContent(f.chapter().getId(),
                        new SaveContentRequest("基于旧版本的覆盖。", v0)));

        assertEquals(409, ex.getCode(), "应返回 409 Conflict");
        assertEquals("第一版正文。",
                chapterRepository.findById(f.chapter().getId()).orElseThrow().getContentMd(),
                "被拒绝的写入不能覆盖已有正文");
    }

    @Test
    @DisplayName("版本历史留痕：每次保存产生一条 HUMAN 版本记录")
    void versionHistoryRecorded() {
        var f = fixtures.full();
        int v0 = f.chapter().getContentVersion();

        chapterService.saveContent(f.chapter().getId(), new SaveContentRequest("正文 A。", v0));
        chapterService.saveContent(f.chapter().getId(), new SaveContentRequest("正文 B。", v0 + 1));

        var versions = chapterService.versions(f.chapter().getId());
        assertTrue(versions.size() >= 2, "应至少留下 2 条版本记录");
        assertTrue(versions.stream().allMatch(v -> "HUMAN".equals(v.editorType())));
    }

    @Test
    @DisplayName("规则①②：前一章未审核、或正在生成时不能生成")
    void canGenerateGate() {
        var f = fixtures.full();

        // 第 1 章允许生成
        assertTrue(chapterService.canGenerate(f.chapter().getId()));

        // 标记为生成中 → 不允许
        var c = chapterRepository.findById(f.chapter().getId()).orElseThrow();
        c.setChapterStatus("GENERATING");
        chapterRepository.save(c);
        assertFalse(chapterService.canGenerate(f.chapter().getId()), "生成中不允许再起任务");

        // 第 2 章：前一章未审核 → 不允许（规则②）
        var c2 = fixtures.newChapter(f.novel(), f.volume(), null, 2);
        assertFalse(chapterService.canGenerate(c2.getId()), "前一章未审核，下一章不能生成");

        // 前一章审核通过 → 允许
        c.setChapterStatus("APPROVED");
        chapterRepository.save(c);
        assertTrue(chapterService.canGenerate(c2.getId()));
    }
}
