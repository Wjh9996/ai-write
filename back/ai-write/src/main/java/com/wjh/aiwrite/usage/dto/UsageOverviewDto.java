package com.wjh.aiwrite.usage.dto;

import java.util.List;

/** 用量总览 */
public record UsageOverviewDto(
        Double monthCost,
        Long monthTokens,
        Integer chapterThisMonth,
        Double budget,
        List<SceneStat> byScene,
        List<UsageRecordDto> records
) {
    public record SceneStat(String scene, Double cost, Integer times) {
    }
}
