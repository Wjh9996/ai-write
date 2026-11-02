package com.wjh.aiwrite.outline.dto;

import com.wjh.aiwrite.outline.model.ChapterOutline;

import java.util.List;

/** 章纲 DTO */
public record OutlineDto(
        Long id,
        Long volumeId,
        Integer chapterNo,
        String title,
        List<String> beats,
        List<String> characters,
        Integer estimatedWords,
        String status,
        Integer version
) {
    public static OutlineDto of(ChapterOutline o) {
        return new OutlineDto(
                o.getId(),
                o.getVolumeId(),
                o.getChapterNo(),
                o.getTitle(),
                o.getBeats() == null ? List.of() : o.getBeats(),
                o.getCharacters() == null ? List.of() : o.getCharacters(),
                o.getEstimatedWords(),
                o.getStatus(),
                o.getVersion()
        );
    }
}
