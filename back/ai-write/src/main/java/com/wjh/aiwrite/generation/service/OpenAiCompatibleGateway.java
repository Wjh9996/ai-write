package com.wjh.aiwrite.generation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjh.aiwrite.config.AiProperties;
import com.wjh.aiwrite.outline.model.ChapterOutline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * OpenAI 兼容网关：DeepSeek / 通义 / 豆包 / Kimi 等都可以直接对接。
 *
 * MVC 定位：基础设施层（Gateway），由 Service 层调用，Controller 不直接依赖它。
 * API Key 只从环境变量 AI_API_KEY 读取，不落盘、不进日志（文档 §9）。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "novel.ai.provider", havingValue = "openai")
public class OpenAiCompatibleGateway implements ModelGateway {

    private final AiProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiCompatibleGateway(AiProperties props, @Qualifier("aiHttpClient") HttpClient http) {
        this.props = props;
        this.http = http;
    }

    @Override
    public void stream(ModelRequest request, Consumer<ModelChunk> onChunk) {
        String payload = chatPayload(request.system(), request.user(), true);
        try {
            // SSE 帧按行解析：以 data: 开头的行才是增量内容
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(sendLines(payload), java.nio.charset.StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data) || data.isEmpty()) continue;
                JsonNode delta = mapper.readTree(data)
                        .path("choices").path(0).path("delta").path("content");
                if (delta.isTextual()) {
                    onChunk.accept(new ModelChunk(delta.asText(), false));
                }
            }
            onChunk.accept(new ModelChunk("", true));
        } catch (Exception e) {
            log.error("[ai] 流式调用失败", e);
            throw new IllegalStateException("模型调用失败：" + e.getMessage(), e);
        }
    }

    @Override
    public List<ChapterOutline> generateOutlines(String prompt, int total, int skip) {
        String json = complete("你是网文主编，只输出 JSON 数组。", prompt);
        return parseOutlines(json, skip);
    }

    @Override
    public String complete(String system, String user) {
        String payload = chatPayload(system, user, false);
        HttpRequest req = HttpRequest.newBuilder(URI.create(props.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(props.timeoutSeconds() == null ? 180 : props.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + Objects.requireNonNullElse(props.apiKey(), ""))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            String body = http.send(req, HttpResponse.BodyHandlers.ofString()).body();
            return mapper.readTree(body)
                    .path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("[ai] 同步调用失败", e);
            throw new IllegalStateException("模型调用失败：" + e.getMessage(), e);
        }
    }

    /** 解析 AI 返回的 JSON 数组；解析失败返回空列表，由上层决定是否重试 */
    private List<ChapterOutline> parseOutlines(String json, int skip) {
        List<ChapterOutline> out = new ArrayList<>();
        String cleaned = json.trim()
                .replaceAll("^```(json)?", "")
                .replaceAll("```$", "")
                .trim();
        try {
            for (JsonNode node : mapper.readTree(cleaned)) {
                ChapterOutline o = new ChapterOutline();
                o.setTitle(node.path("title").asText("未命名"));
                List<String> beats = new ArrayList<>();
                node.path("beats").forEach(b -> beats.add(b.asText()));
                // 不合规则的 beat 数直接跳过，交给上层提示重拆
                if (beats.size() < 3 || beats.size() > 7) continue;
                o.setBeats(beats);
                List<String> chars = new ArrayList<>();
                node.path("characters").forEach(c -> chars.add(c.asText()));
                o.setCharacters(chars);
                int words = node.path("words").asInt(2500);
                o.setEstimatedWords(words <= 0 ? 2500 : words);
                out.add(o);
            }
        } catch (Exception e) {
            log.warn("[ai] 章节 JSON 解析失败，返回空列表：{}", e.getMessage());
        }
        return out;
    }

    /** 组装 /chat/completions 请求体 */
    private String chatPayload(String system, String user, boolean stream) {
        return """
                {
                  "model": "%s",
                  "stream": %s,
                  "messages": [
                    {"role": "system", "content": %s},
                    {"role": "user", "content": %s}
                  ]
                }
                """.formatted(props.model(), stream, quote(system), quote(user));
    }

    private java.io.InputStream sendLines(String payload) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(props.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(props.timeoutSeconds() == null ? 180 : props.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + Objects.requireNonNullElse(props.apiKey(), ""))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofInputStream()).body();
    }

    private String quote(String s) {
        try {
            return mapper.writeValueAsString(s == null ? "" : s);
        } catch (Exception e) {
            return "\"\"";
        }
    }
}
