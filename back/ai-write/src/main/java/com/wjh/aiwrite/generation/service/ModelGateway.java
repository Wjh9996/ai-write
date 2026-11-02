package com.wjh.aiwrite.generation.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 模型网关（文档 §6 定义的接口）
 *
 * 设计原则：不绑定单一厂商，支持流式输出、超时与重试，模型 / 价格可配置。
 * 实现类：
 *   - MockModelGateway：用本地语料流式吐字，无 Key、无网络也能跑通整条闭环
 *   - OpenAiCompatibleGateway：任意 OpenAI 兼容接口（DeepSeek / 通义 / 豆包 / Kimi …）
 */
public interface ModelGateway {

    /**
     * 流式生成一段正文
     *
     * @param request        请求（提示词、目标字数、参数）
     * @param onChunk        增量回调（必须是非阻塞的，内部会在其线程上等待）
     */
    void stream(ModelRequest request, Consumer<ModelChunk> onChunk);

    /**
     * 按提示词生成结构化内容：AI 拆章纲
     *
     * @param prompt     提示词（含总纲 / 本卷事件 / 设定库摘要）
     * @param total      期望章数
     * @param skip       已确认的章数（这部分不再重复生成）
     */
    List<com.wjh.aiwrite.outline.model.ChapterOutline> generateOutlines(String prompt, int total, int skip);

    /** 一次性生成：一致性自检 / 摘要草稿 */
    String complete(String system, String user);

    record ModelRequest(String system, String user, int targetWords, String model) {
    }

    record ModelChunk(String delta, boolean done) {
    }
}
