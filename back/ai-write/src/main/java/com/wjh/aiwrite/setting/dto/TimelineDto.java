package com.wjh.aiwrite.setting.dto;

import com.wjh.aiwrite.setting.model.TimelineEvent;

/** 时间线 DTO */
public record TimelineDto(
        Long id, Long novelId, Long chapterId, String timeLabel, String event,
        String approvalStatus, Integer sort
) {
    public static TimelineDto of(TimelineEvent t) {
        return new TimelineDto(t.getId(), t.getNovelId(), t.getChapterId(), t.getTimeLabel(),
                t.getEvent(), t.getApprovalStatus(), t.getSortNo());
    }
}
