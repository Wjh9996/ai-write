package com.wjh.aiwrite.review.dto;

import java.util.List;

/** 审核草稿（自检 + 摘要 + 变更） */
public record ReviewDraftDto(
        Long chapterId,
        List<IssueDto> issues,
        SummaryDto summary,
        List<ChangeDto> changes,
        Integer wordCount,
        String createdAt
) {
    public record SummaryDto(String content, Integer sourceContentVersion) {
    }
}
