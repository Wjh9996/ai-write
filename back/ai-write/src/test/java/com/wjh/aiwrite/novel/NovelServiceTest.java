package com.wjh.aiwrite.novel;

import com.wjh.aiwrite.novel.dto.CreateNovelRequest;
import com.wjh.aiwrite.novel.dto.NovelDto;
import com.wjh.aiwrite.novel.service.NovelService;
import com.wjh.aiwrite.outline.repository.VolumeRepository;
import com.wjh.aiwrite.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 作品服务测试：创建即建第一卷、统计口径、TXT 导入解析
 */
@SpringBootTest
@ActiveProfiles("h2")
class NovelServiceTest {

    @Autowired
    private NovelService novelService;
    @Autowired
    private VolumeRepository volumeRepository;
    @Autowired
    private TestFixtures fixtures;

    @Test
    @DisplayName("新建作品：自动建第一卷，避免用户进大纲页没落点")
    void createCreatesFirstVolume() {
        NovelDto dto = novelService.create(1L,
                new CreateNovelRequest("新作品-" + System.nanoTime(), "简介", "主线"));

        assertNotNull(dto.id());
        assertEquals(1, volumeRepository.findByNovelIdOrderByVolumeNo(dto.id()).size(),
                "新作品应自动带一卷");
        assertEquals("第一卷 · 开端",
                volumeRepository.findByNovelIdOrderByVolumeNo(dto.id()).get(0).getTitle());
    }

    @Test
    @DisplayName("统计：按章节聚合字数与状态")
    void statsAggregation() {
        var f = fixtures.full();

        var stats = novelService.statsOf(f.novel().getId());

        assertEquals(1, stats.chapterCount());
        assertEquals(0, stats.approvedCount());
        assertEquals(0, stats.totalWords());
        assertEquals(1, stats.volumeCount());
    }

    @Test
    @DisplayName("作品列表按 user_id 隔离（规则⑧）")
    void listScopedByUser() {
        fixtures.full();
        List<NovelDto> mine = novelService.list(1L);
        List<NovelDto> others = novelService.list(999L);

        assertFalse(mine.isEmpty());
        assertTrue(others.isEmpty(), "别人的列表里不能出现我的作品");
    }
}
