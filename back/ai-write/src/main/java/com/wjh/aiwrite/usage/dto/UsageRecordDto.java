package com.wjh.aiwrite.usage.dto;

import com.wjh.aiwrite.usage.model.UsageRecord;

/** 用量明细 */
public record UsageRecordDto(
        Long id, String jobId, String scene, String model,
        Integer inputTokens, Integer outputTokens, Double estimatedCost, String createdAt
) {
    public static UsageRecordDto of(UsageRecord r) {
        return new UsageRecordDto(
                r.getId(), r.getJobId(), r.getScene(), r.getModel(),
                r.getInputTokens(), r.getOutputTokens(),
                r.getEstimatedCost() == null ? 0.0 : r.getEstimatedCost().doubleValue(),
                String.valueOf(r.getCreatedAt())
        );
    }
}
