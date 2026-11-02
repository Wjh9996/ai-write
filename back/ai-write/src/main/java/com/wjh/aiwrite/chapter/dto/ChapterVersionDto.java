package com.wjh.aiwrite.chapter.dto;

import com.wjh.aiwrite.chapter.model.ChapterVersion;

/** 章节版本历史 */
public record ChapterVersionDto(
        Long id,
        Long chapterId,
        Integer versionNo,
        String contentMd,
        String editorType,
        String createdAt
) {
    public static ChapterVersionDto of(ChapterVersion v) {
        return new ChapterVersionDto(
                v.getId(),
                v.getChapterId(),
                v.getVersionNo(),
                v.getContentMd(),
                v.getEditorType(),
                String.valueOf(v.getCreatedAt())
        );
    }
}
