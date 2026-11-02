package com.wjh.aiwrite.novel.dto;

/**
 * 作品统计（派生数据，列表接口返回）
 */
public record NovelStatsDto(
        Integer totalWords,
        Integer chapterCount,
        Integer approvedCount,
        Integer reviewPendingCount,
        Integer generatingCount,
        Integer outlinedCount,
        Integer volumeCount
) {
    public static NovelStatsDto empty() {
        return new NovelStatsDto(0, 0, 0, 0, 0, 0, 0);
    }
}
