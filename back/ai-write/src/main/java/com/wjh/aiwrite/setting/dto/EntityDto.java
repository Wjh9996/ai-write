package com.wjh.aiwrite.setting.dto;

import com.wjh.aiwrite.setting.model.SettingEntity;

import java.util.List;
import java.util.Map;

/** 设定实体卡 DTO */
public record EntityDto(
        Long id,
        Long novelId,
        String type,
        String name,
        List<String> aliases,
        Map<String, String> card,
        String source,
        String approvalStatus,
        Integer revealChapter,
        String updatedAt
) {
    public static EntityDto of(SettingEntity e) {
        return new EntityDto(
                e.getId(),
                e.getNovelId(),
                e.getEntityType(),
                e.getName(),
                e.getAliases() == null ? List.of() : e.getAliases(),
                e.getCard() == null ? Map.of() : e.getCard(),
                e.getSource(),
                e.getApprovalStatus(),
                e.getRevealChapter(),
                String.valueOf(e.getUpdatedAt())
        );
    }
}
