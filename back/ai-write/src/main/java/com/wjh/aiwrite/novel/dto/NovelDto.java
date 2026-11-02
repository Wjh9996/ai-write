package com.wjh.aiwrite.novel.dto;

import com.wjh.aiwrite.novel.model.Novel;

/**
 * 作品 DTO（View Model）—— 与前端 Novel 模型字段一一对应
 */
public record NovelDto(
        Long id,
        Long userId,
        String title,
        String intro,
        String mainOutline,
        String coverGradient,
        String status,
        Integer version,
        String createdAt,
        String updatedAt,
        NovelStatsDto stats,
        Integer wordCount
) {
    private static final String[] GRADIENTS = {
            "linear-gradient(135deg,#6366f1 0%,#a855f7 45%,#ec4899 100%)",
            "linear-gradient(135deg,#38bdf8 0%,#6366f1 100%)",
            "linear-gradient(135deg,#34d399 0%,#22d3ee 100%)",
            "linear-gradient(135deg,#fb923c 0%,#f43f5e 100%)",
            "linear-gradient(135deg,#fbbf24 0%,#f97316 100%)"
    };

    public static NovelDto of(Novel n, NovelStatsDto stats) {
        return new NovelDto(
                n.getId(),
                n.getUserId(),
                n.getTitle(),
                n.getIntro(),
                n.getMainOutline(),
                GRADIENTS[(int) (n.getId() % GRADIENTS.length)],
                n.getStatus(),
                n.getVersion(),
                String.valueOf(n.getCreatedAt()),
                String.valueOf(n.getUpdatedAt()),
                stats,
                stats == null ? 0 : stats.totalWords()
        );
    }
}
