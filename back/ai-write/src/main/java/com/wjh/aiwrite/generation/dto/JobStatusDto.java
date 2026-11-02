package com.wjh.aiwrite.generation.dto;

/** 任务状态（前端 job 面板用） */
public record JobStatusDto(
        String id,
        Long chapterId,
        String status,
        Integer currentSegment,
        Integer totalSegments,
        String model,
        String promptVersion,
        String errorCode,
        String createdAt,
        String updatedAt
) {
}
