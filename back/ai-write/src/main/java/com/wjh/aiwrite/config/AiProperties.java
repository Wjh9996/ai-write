package com.wjh.aiwrite.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 网关与生成参数配置（application.yml: novel.ai.*）
 */
@ConfigurationProperties(prefix = "novel.ai")
public record AiProperties(
        String provider,
        String model,
        String baseUrl,
        String apiKey,
        Integer timeoutSeconds,
        Integer maxRetry,
        Generation generation,
        Cost cost
) {
    public record Generation(
            Integer chapterTargetWords,
            Integer chapterMinWords,
            Integer chapterMaxWords,
            String segmentTargets,
            Integer streamIntervalMs
    ) {
        /** "500,550,450,450,550" -> List<Integer> */
        public List<Integer> segmentTargetList() {
            List<Integer> list = new ArrayList<>();
            if (segmentTargets == null || segmentTargets.isBlank()) {
                return List.of(500, 500, 500, 500, 500);
            }
            for (String part : segmentTargets.split(",")) {
                String v = part.trim();
                if (!v.isEmpty()) {
                    list.add(Integer.parseInt(v));
                }
            }
            return List.copyOf(list);
        }
    }

    public record Cost(Double inputPricePerK, Double outputPricePerK, Double monthBudget) {
    }
}
