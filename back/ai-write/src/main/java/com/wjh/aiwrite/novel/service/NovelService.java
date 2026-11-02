package com.wjh.aiwrite.novel.service;

import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.novel.dto.CreateNovelRequest;
import com.wjh.aiwrite.novel.dto.NovelDto;
import com.wjh.aiwrite.novel.dto.NovelStatsDto;
import com.wjh.aiwrite.novel.model.Novel;
import com.wjh.aiwrite.novel.repository.NovelRepository;
import com.wjh.aiwrite.outline.model.Volume;
import com.wjh.aiwrite.outline.repository.VolumeRepository;
import com.wjh.aiwrite.support.TextKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 作品服务（Model 层业务）
 *
 * MVC 分工：Controller 只负责 HTTP，业务规则全部落在这里。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;
    private final VolumeRepository volumeRepository;
    private final ChapterRepository chapterRepository;

    public List<NovelDto> list(Long userId) {
        return novelRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(n -> NovelDto.of(n, statsOf(n.getId())))
                .toList();
    }

    public NovelDto detail(Long novelId) {
        return NovelDto.of(require(novelId), statsOf(novelId));
    }

    @Transactional
    public NovelDto create(Long userId, CreateNovelRequest req) {
        Novel novel = new Novel();
        novel.setUserId(userId);
        novel.setTitle(req.title().trim());
        novel.setIntro(req.intro() == null ? "" : req.intro());
        novel.setMainOutline(req.mainOutline());
        novel.setStatus("DRAFT");
        Novel saved = novelRepository.save(novel);

        // 新作品自动建第一卷，保证进入大纲页有落点
        Volume v = new Volume();
        v.setNovel(saved);
        v.setVolumeNo(1);
        v.setTitle("第一卷 · 开端");
        v.setStatus("RAW");
        v.setTargetChapters(18);
        v.setEvents(new ArrayList<>());
        volumeRepository.save(v);

        log.info("[novel] created id={} title={}", saved.getId(), saved.getTitle());
        return NovelDto.of(saved, NovelStatsDto.empty());
    }

    @Transactional
    public NovelDto updateOutline(Long novelId, String mainOutline) {
        Novel novel = require(novelId);
        novel.setMainOutline(mainOutline);
        novelRepository.save(novel);
        return NovelDto.of(novel, statsOf(novelId));
    }

    /**
     * TXT 大纲导入：限制 2MB，UTF-8 优先、GBK 兜底；识别「第X卷」重建卷列表
     */
    @Transactional
    public NovelDto importTxt(Long novelId, MultipartFile file) {
        Novel novel = require(novelId);
        novel.setMainOutline("[导入自 " + file.getOriginalFilename() + "]\n" + readText(file));

        TextKit.ParsedOutline parsed = TextKit.parseTxtOutline(novel.getMainOutline());
        if (!parsed.getVolumeTitles().isEmpty()) {
            volumeRepository.findByNovelIdOrderByVolumeNo(novelId)
                    .forEach(v -> volumeRepository.deleteById(v.getId()));
            int no = 1;
            for (String title : parsed.getVolumeTitles()) {
                Volume v = new Volume();
                v.setNovel(novel);
                v.setVolumeNo(no++);
                v.setTitle(title.length() > 30 ? title.substring(0, 30) : title);
                v.setStatus("RAW");
                v.setTargetChapters(18);
                v.setEvents(new ArrayList<>());
                volumeRepository.save(v);
            }
        }
        novelRepository.save(novel);
        return NovelDto.of(novel, statsOf(novelId));
    }

    @Transactional
    public void remove(Long novelId) {
        novelRepository.delete(require(novelId));
    }

    /** 派生统计：从章节聚合，避免冗余字段不一致 */
    public NovelStatsDto statsOf(Long novelId) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNo(novelId);
        int words = 0, approved = 0, pending = 0, generating = 0;
        for (Chapter c : chapters) {
            words += c.getWordCount() == null ? 0 : c.getWordCount();
            switch (c.getChapterStatus()) {
                case "APPROVED" -> approved++;
                case "REVIEW_PENDING" -> pending++;
                case "GENERATING" -> generating++;
                default -> {
                }
            }
        }
        return new NovelStatsDto(
                words,
                chapters.size(),
                approved,
                pending,
                generating,
                chapters.size(),
                volumeRepository.countByNovelId(novelId)
        );
    }

    private Novel require(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("作品不存在"));
    }

    private String readText(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            try {
                return new String(file.getBytes(), Charset.forName("GBK"));
            } catch (Exception ex) {
                throw new IllegalArgumentException("文件读取失败：" + ex.getMessage());
            }
        }
    }
}
