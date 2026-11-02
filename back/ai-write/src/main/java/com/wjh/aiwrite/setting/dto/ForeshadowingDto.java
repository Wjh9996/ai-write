package com.wjh.aiwrite.setting.dto;

import com.wjh.aiwrite.setting.model.Foreshadowing;

/** 伏笔 DTO */
public record ForeshadowingDto(
        Long id, Long novelId, String content, Long setChapterId, Long payoffChapterId,
        String status, String source, String approvalStatus
) {
    public static ForeshadowingDto of(Foreshadowing f) {
        return new ForeshadowingDto(f.getId(), f.getNovelId(), f.getContent(), f.getSetChapterId(),
                f.getPayoffChapterId(), f.getStatus(), f.getSource(), f.getApprovalStatus());
    }
}
