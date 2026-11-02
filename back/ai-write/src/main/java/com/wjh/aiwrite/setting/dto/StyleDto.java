package com.wjh.aiwrite.setting.dto;

import com.wjh.aiwrite.setting.model.StyleProfile;

import java.util.List;
import java.util.Map;

/** 文风卡 DTO */
public record StyleDto(
        Long id, Long novelId, Map<String, String> card, String sampleText, List<String> forbidden
) {
    public static StyleDto of(StyleProfile p) {
        return new StyleDto(p.getId(), p.getNovelId(),
                p.getCard() == null ? Map.of() : p.getCard(),
                p.getSampleText(),
                p.getForbidden() == null ? List.of() : p.getForbidden());
    }
}
