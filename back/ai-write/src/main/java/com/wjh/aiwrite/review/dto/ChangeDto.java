package com.wjh.aiwrite.review.dto;

/** 设定变更草稿条目 */
public record ChangeDto(
        String id,
        String kind,
        String name,
        String change,
        String time,
        String action,
        String entityType,
        Boolean accepted
) {
    public ChangeDto(String id, String kind, String name, String change,
                     String time, String action, String entityType) {
        this(id, kind, name, change, time, action, entityType, true);
    }
}
