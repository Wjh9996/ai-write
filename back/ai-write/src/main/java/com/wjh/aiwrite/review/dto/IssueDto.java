package com.wjh.aiwrite.review.dto;

/** 自检问题（带正文引文，文档 §4.4） */
public record IssueDto(
        String id,
        String type,
        String level,
        String quote,
        String issue,
        String suggest,
        Boolean resolved
) {
    public IssueDto(String id, String type, String level, String quote, String issue, String suggest) {
        this(id, type, level, quote, issue, suggest, false);
    }
}
