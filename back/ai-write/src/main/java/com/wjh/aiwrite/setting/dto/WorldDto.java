package com.wjh.aiwrite.setting.dto;

import com.wjh.aiwrite.setting.model.WorldSetting;

/** 世界观 DTO */
public record WorldDto(
        Long id, Long novelId, String category, String title, String content,
        String source, String approvalStatus
) {
    public static WorldDto of(WorldSetting w) {
        return new WorldDto(w.getId(), w.getNovelId(), w.getCategory(), w.getTitle(),
                w.getContent(), w.getSource(), w.getApprovalStatus());
    }
}
