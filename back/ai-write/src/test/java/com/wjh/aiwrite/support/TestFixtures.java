package com.wjh.aiwrite.support;

import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.novel.model.Novel;
import com.wjh.aiwrite.novel.repository.NovelRepository;
import com.wjh.aiwrite.outline.model.ChapterOutline;
import com.wjh.aiwrite.outline.model.Volume;
import com.wjh.aiwrite.outline.repository.OutlineRepository;
import com.wjh.aiwrite.outline.repository.VolumeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试夹具：造一部作品 + 一卷 + 已确认章纲 + 对应章节
 *
 * 集成测试统一走 H2（profile=h2，ddl-auto=create-drop），不依赖真实 MySQL，
 * 这样本机和 CI 都能跑。
 */
@Component
public class TestFixtures {

    @Autowired
    private NovelRepository novelRepository;
    @Autowired
    private VolumeRepository volumeRepository;
    @Autowired
    private OutlineRepository outlineRepository;
    @Autowired
    private ChapterRepository chapterRepository;

    public Novel newNovel(String title) {
        Novel n = new Novel();
        n.setUserId(1L);
        n.setTitle(title);
        n.setIntro("测试用作品");
        n.setMainOutline("主线：捡蛋 → 孵化 → 出山 → 成名");
        n.setStatus("WRITING");
        return novelRepository.save(n);
    }

    public Volume newVolume(Novel novel, String title) {
        Volume v = new Volume();
        v.setNovel(novel);
        v.setVolumeNo(1);
        v.setTitle(title);
        v.setStatus("OUTLINED");
        v.setTargetChapters(3);
        v.setEvents(new ArrayList<>(List.of("发现灰蛋", "孵化失败", "破壳认主")));
        return volumeRepository.save(v);
    }

    public ChapterOutline newOutline(Volume v, int no, String title, List<String> beats) {
        ChapterOutline o = new ChapterOutline();
        o.setVolumeId(v.getId());
        o.setChapterNo(no);
        o.setTitle(no + ". " + title);
        o.setBeats(new ArrayList<>(beats));
        o.setCharacters(new ArrayList<>(List.of("林昭", "灰羽")));
        o.setEstimatedWords(2500);
        o.setStatus("CONFIRMED");
        return outlineRepository.save(o);
    }

    public Chapter newChapter(Novel novel, Volume v, ChapterOutline outline, int no) {
        Chapter c = new Chapter();
        c.setNovelId(novel.getId());
        c.setVolumeId(v.getId());
        c.setOutlineId(outline == null ? null : outline.getId());
        c.setChapterNo(no);
        c.setTitle(no + ". 测试章");
        c.setContentMd("");
        c.setWordCount(0);
        c.setChapterStatus("OUTLINED");
        c.setAuditStatus("NOT_RUN");
        c.setContentVersion(1);
        return chapterRepository.save(c);
    }

    /** 一整套：作品 + 卷 + 章纲 + 第 1 章（可生成状态） */
    public Fixture full() {
        Novel novel = newNovel("测试作品-" + System.nanoTime());
        Volume volume = newVolume(novel, "第一卷 · 测试");
        ChapterOutline o1 = newOutline(volume, 1, "破壳之前",
                List.of("开场铺垫", "发现灰蛋", "章末钩子"));
        Chapter c1 = newChapter(novel, volume, o1, 1);
        return new Fixture(novel, volume, o1, c1);
    }

    public record Fixture(Novel novel, Volume volume, ChapterOutline outline, Chapter chapter) {
    }
}
