package com.wjh.aiwrite.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字数统计与 TXT 解析的单元测试（不需要 Spring 上下文，纯函数校验）
 *
 * 文档 §11：「中文『字数』统计口径」是明确列为待验证的风险项，
 * 这里把口径固定下来：中文字符 + 英文单词 + 数字串，与前端 countWords 保持一致。
 */
class TextKitTest {

    @Test
    @DisplayName("中文按字计，标点不计")
    void countChineseOnly() {
        assertEquals(5, TextKit.countWords("矿洞里的水"));
        assertEquals(5, TextKit.countWords("矿洞里的水。！！…"), "标点不计入字数");
    }

    @Test
    @DisplayName("英文按单词计，数字按串计")
    void countEnglishAndNumber() {
        assertEquals(3, TextKit.countWords("hello world ai"));
        assertEquals(1, TextKit.countWords("2026"));
        assertEquals(3, TextKit.countWords("第2026章"), "2 个汉字 + 1 个数字串");
    }

    @Test
    @DisplayName("空值与 null 返回 0")
    void countEmpty() {
        assertEquals(0, TextKit.countWords(null));
        assertEquals(0, TextKit.countWords(""));
    }

    @Test
    @DisplayName("切块保持标点完整，且不超过最大长度")
    void splitChunks() {
        String text = "第一句。第二句！第三句？第四句；第五句。";
        var chunks = TextKit.splitChunks(text, 2, 8);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().allMatch(c -> c.length() <= 8));
        // 拼回去应与原文一致，不能丢字
        assertEquals(text, String.join("", chunks));
    }

    @Test
    @DisplayName("TXT 总纲能识别出卷标题")
    void parseVolumeTitles() {
        String raw = """
                【第一卷·废矿里的灰蛋】林昭被发配看守废弃矿洞。
                第二卷 · 宝宝赛：初入低级学府测试场。
                随便一行没有卷号的内容
                """;
        TextKit.ParsedOutline po = TextKit.parseTxtOutline(raw);
        assertEquals(2, po.getVolumeTitles().size());
        assertTrue(po.getVolumeTitles().get(0).contains("第一卷"));
        assertTrue(po.getMainOutline().contains("废矿"));
    }

    @Test
    @DisplayName("没有卷号时只保留总纲")
    void parseWithoutVolumes() {
        var po = TextKit.parseTxtOutline("主线：捡蛋 → 孵化 → 成名");
        assertTrue(po.getVolumeTitles().isEmpty());
    }
}
