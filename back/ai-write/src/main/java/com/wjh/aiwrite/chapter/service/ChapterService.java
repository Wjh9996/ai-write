package com.wjh.aiwrite.chapter.service;

import com.wjh.aiwrite.chapter.dto.ChapterDto;
import com.wjh.aiwrite.chapter.dto.ChapterVersionDto;
import com.wjh.aiwrite.chapter.dto.SaveContentRequest;
import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.model.ChapterVersion;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.chapter.repository.ChapterVersionRepository;
import com.wjh.aiwrite.common.BizException;
import com.wjh.aiwrite.support.TextKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 章节服务（Model 层业务）
 *
 * 规则④：content_md 是正文唯一权威版本；
 * 规则⑤：AI 不得直接覆盖人工编辑 —— 这里用 expectedVersion 做乐观锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final ChapterVersionRepository versionRepository;

    public List<ChapterDto> list(Long novelId) {
        return chapterRepository.findByNovelIdOrderByChapterNo(novelId)
                .stream().map(ChapterDto::of).toList();
    }

    public ChapterDto detail(Long id) {
        return ChapterDto.of(require(id));
    }

    /**
     * 保存 Markdown 正文，携带 expectedVersion 做乐观锁（文档 §6）
     */
    @Transactional
    public ChapterDto saveContent(Long id, SaveContentRequest req) {
        Chapter c = require(id);
        int expected = req.expectedVersion() == null ? c.getContentVersion() : req.expectedVersion();
        if (expected != c.getContentVersion()) {
            // 409：正文已被其他操作更新，前端提示刷新
            throw BizException.conflict("正文已被其他操作更新，请刷新后再保存");
        }

        // 版本历史留痕（规则：编辑可追溯）
        ChapterVersion v = new ChapterVersion();
        v.setChapterId(id);
        v.setVersionNo(c.getContentVersion());
        v.setContentMd(c.getContentMd());
        v.setEditorType("HUMAN");
        versionRepository.save(v);

        c.setContentMd(req.contentMd());
        c.setWordCount(TextKit.countWords(req.contentMd()));
        c.setContentVersion(c.getContentVersion() + 1);
        return ChapterDto.of(chapterRepository.save(c));
    }

    public List<ChapterVersionDto> versions(Long id) {
        return versionRepository.findByChapterIdOrderByVersionNoDesc(id)
                .stream().map(ChapterVersionDto::of).toList();
    }

    /** 权限校验用 */
    public Long novelIdOf(Long chapterId) {
        return require(chapterId).getNovelId();
    }

    /**
     * 是否可以生成本章（前端按钮的禁用依据）
     * 对应不可妥协规则①②⑦
     */
    public boolean canGenerate(Long chapterId) {
        Chapter c = require(chapterId);
        if ("GENERATING".equals(c.getChapterStatus())) {
            return false;
        }
        if (c.getChapterNo() > 1) {
            boolean prevApproved = chapterRepository
                    .findByNovelIdOrderByChapterNo(c.getNovelId()).stream()
                    .filter(x -> x.getChapterNo() == c.getChapterNo() - 1)
                    .findFirst()
                    .map(x -> "APPROVED".equals(x.getChapterStatus()))
                    .orElse(false);
            if (!prevApproved) {
                return false;
            }
        }
        return true;
    }

    private Chapter require(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("章节不存在"));
    }
}
