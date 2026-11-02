package com.wjh.aiwrite.chapter.dto;

import com.wjh.aiwrite.chapter.model.Chapter;

/** 章节 DTO（与前端 Chapter 字段一致） */
public record ChapterDto(
        Long id,
        Long novelId,
        Long volumeId,
        Long outlineId,
        Integer chapterNo,
        String title,
        String contentMd,
        Integer wordCount,
        String chapterStatus,
        String auditStatus,
        Integer contentVersion,
        String createdAt,
        String updatedAt
) {
    public static ChapterDto of(Chapter c) {
        return new ChapterDto(
                c.getId(),
                c.getNovelId(),
                c.getVolumeId(),
                c.getOutlineId(),
                c.getChapterNo(),
                c.getTitle(),
                c.getContentMd(),
                c.getWordCount(),
                c.getChapterStatus(),
                c.getAuditStatus(),
                c.getContentVersion(),
                String.valueOf(c.getCreatedAt()),
                String.valueOf(c.getUpdatedAt())
        );
    }
}
