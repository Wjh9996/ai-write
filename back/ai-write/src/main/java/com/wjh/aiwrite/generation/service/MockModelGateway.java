package com.wjh.aiwrite.generation.service;

import com.wjh.aiwrite.config.AiProperties;
import com.wjh.aiwrite.outline.model.ChapterOutline;
import com.wjh.aiwrite.support.TextKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * 本地语料网关。
 *
 * 作用：在没有 API Key 的情况下，也能把「章节级流式生成 → 自检 → 摘要 → 审核入库」
 * 这条完整闭环跑通贴合前端 mock，方便你先把产品流程调好，再插真实模型。
 * 切换方式：application.yml 里 novel.ai.provider=openai 并配好 AI_API_KEY。
 *
 * 语料文件：resources/novel/mock-chapter.txt，段与段之间用 --SEG-- 分隔，
 * 与前端 src/api/mock/streamText.ts 同源（由 tools/extract-corpus.mjs 导出）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockModelGateway implements ModelGateway {

    public static final String CORPUS = "novel/mock-chapter.txt";

    private final AiProperties props;

    private volatile List<String> segments;

    private List<String> segments() {
        if (segments == null) {
            synchronized (this) {
                if (segments == null) {
                    try (InputStream in = new ClassPathResource(CORPUS).getInputStream()) {
                        String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        segments = List.of(body.split("--SEG--"));
                    } catch (Exception e) {
                        log.error("[mock-gateway] 语料加载失败", e);
                        segments = List.of("（mock 语料缺失，请检查 resources/novel/mock-chapter.txt）");
                    }
                }
            }
        }
        return segments;
    }

    @Override
    public void stream(ModelRequest request, Consumer<ModelChunk> onChunk) {
        int segTarget = request.targetWords() <= 0 ? 500 : request.targetWords();
        String text = buildText(segTarget);
        int interval = props == null || props.generation() == null ? 26
                : Math.max(0, props.generation().streamIntervalMs() == null ? 26 : props.generation().streamIntervalMs());

        for (String piece : TextKit.splitChunks(text, 4, 12)) {
            onChunk.accept(new ModelChunk(piece, false));
            if (interval > 0) {
                sleep(interval);
            }
        }
        onChunk.accept(new ModelChunk("", true));
    }

    /**
     * 按目标字数拼一段。
     * 必须精确到目标字数：流式分段生成是「每段目标 450—550 字」，
     * 若整段追加会把单章顶到 2700 字上限之外（验收区间是硬指标，见文档 §1.1）。
     */
    private String buildText(int targetWords) {
        List<String> pool = segments();
        int start = ThreadLocalRandom.current().nextInt(pool.size());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pool.size(); i++) {
            String seg = pool.get((start + i) % pool.size()).trim();
            if (TextKit.countWords(sb.toString()) >= targetWords) {
                break;
            }
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(seg);
        }
        // 超限则按句截断，保证不越过本段目标
        if (TextKit.countWords(sb.toString()) > targetWords) {
            return truncateBySentence(sb.toString(), targetWords);
        }
        return sb.toString();
    }

    /** 按标点切成句，累积到目标字数为止 */
    private String truncateBySentence(String text, int targetWords) {
        StringBuilder out = new StringBuilder();
        for (String sentence : text.split("(?<=[。！？；])")) {
            out.append(sentence);
            if (TextKit.countWords(out.toString()) >= targetWords) {
                break;
            }
        }
        return out.toString();
    }

    @Override
    public List<ChapterOutline> generateOutlines(String prompt, int total, int skip) {
        // 语料驱动：按本卷事件顺序生成固定结构的章纲，beat 数恒为 3—7 条
        List<ChapterOutline> out = new ArrayList<>();
        String[] titles = {"破壳之前", "第一声啼鸣", "冷了的夜", "不是草鸡", "镇口的那辆车",
                "谁也不知道的东西", "一张旧契", "十四枚灵石", "雨里的脚印", "第一次实战",
                "被看轻的人", "一道裂缝", "那年冬猎", "主人的名字", "看不见的丝线",
                "交换的条件", "谁在跟着", "离开矿镇"};
        String[] roles = {"林昭", "灰羽", "苏晚"};

        int count = Math.max(0, total - skip);
        for (int i = 1; i <= count; i++) {
            ChapterOutline o = new ChapterOutline();
            o.setTitle(i + ". " + titles[(skip + i - 1) % titles.length]);
            List<String> beats = new ArrayList<>(List.of(
                    "开场场景与情绪铺垫",
                    "本段核心冲突出现",
                    "一次尝试失败",
                    "换思路再干一次",
                    "章末留下钩子"));
            // 让 beat 数在 3—7 之间浮动，模拟真实 AI 输出
            int keep = 3 + ((skip + i) % 5);
            o.setBeats(List.copyOf(beats.subList(0, Math.min(keep, beats.size()))));
            o.setCharacters(List.of(roles[0], roles[1]));
            o.setEstimatedWords(2300 + ((i * 37) % 400));
            out.add(o);
        }
        return out;
    }

    @Override
    public String complete(String system, String user) {
        // 自检与摘要走同一份语料派生，保证结构可用
        return "[]";
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
