package com.wjh.aiwrite.usage.service;

import com.wjh.aiwrite.usage.dto.UsageOverviewDto;
import com.wjh.aiwrite.usage.dto.UsageRecordDto;
import com.wjh.aiwrite.usage.model.UsageRecord;
import com.wjh.aiwrite.usage.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用量统计（Model 层业务）—— 文档 §9：每次模型调用都记账
 */
@Service
@RequiredArgsConstructor
public class UsageService {

    private static final double BUDGET = 100.0;

    private final UsageRecordRepository repository;

    public UsageOverviewDto overview() {
        List<UsageRecord> records = repository.findTop100ByOrderByCreatedAtDesc();

        double cost = records.stream()
                .map(r -> r.getEstimatedCost() == null ? BigDecimal.ZERO : r.getEstimatedCost())
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        long tokens = records.stream()
                .mapToLong(r -> (r.getInputTokens() == null ? 0 : r.getInputTokens())
                        + (r.getOutputTokens() == null ? 0 : r.getOutputTokens()))
                .sum();

        Map<String, UsageOverviewDto.SceneStat> sceneMap = new LinkedHashMap<>();
        for (UsageRecord r : records) {
            UsageOverviewDto.SceneStat s = sceneMap.computeIfAbsent(r.getScene(),
                    k -> new UsageOverviewDto.SceneStat(k, 0.0, 0));
            sceneMap.put(r.getScene(), new UsageOverviewDto.SceneStat(
                    r.getScene(),
                    s.cost() + (r.getEstimatedCost() == null ? 0 : r.getEstimatedCost().doubleValue()),
                    s.times() + 1));
        }

        return new UsageOverviewDto(
                BigDecimal.valueOf(cost).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue(),
                tokens,
                (int) records.stream().filter(r -> "分段生成".equals(r.getScene())).count(),
                BUDGET,
                List.copyOf(sceneMap.values()),
                records.stream().map(UsageRecordDto::of).toList()
        );
    }
}
