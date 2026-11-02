package com.wjh.aiwrite.review.controller;

import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.review.dto.ApproveRequest;
import com.wjh.aiwrite.review.dto.ReviewDraftDto;
import com.wjh.aiwrite.review.service.ReviewService;
import com.wjh.aiwrite.support.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审核 Controller（View 层）—— 文档 §6：review-draft / approve
 */
@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ChapterRepository chapterRepository;
    private final Ownership ownership;

    /** 按最新正文自检并生成摘要 / 变更草稿 */
    @PostMapping("/{id}/review-draft")
    public ApiResult<ReviewDraftDto> draft(@PathVariable Long id) {
        ownership.requireNovel(chapterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("章节不存在")).getNovelId());
        return ApiResult.ok(reviewService.draft(id));
    }

    /** 通过并入库：事务写回摘要 / 角色状态 / 时间线 / 伏笔 */
    @PostMapping("/{id}/approve")
    public ApiResult<Void> approve(@PathVariable Long id, @RequestBody ApproveRequest req) {
        ownership.requireNovel(chapterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("章节不存在")).getNovelId());
        reviewService.approve(id, req);
        return ApiResult.ok(null, "已入库，下一章现在可以生成");
    }

    @PostMapping("/{id}/reject")
    public ApiResult<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ownership.requireNovel(chapterRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("章节不存在")).getNovelId());
        reviewService.reject(id, body.getOrDefault("reason", ""));
        return ApiResult.ok(null, "已退回修改");
    }
}
