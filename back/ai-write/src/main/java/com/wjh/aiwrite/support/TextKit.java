package com.wjh.aiwrite.support;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用工具：字数统计与 TXT 解析
 */

public final class TextKit {

    private TextKit() {
    }

    private static final Pattern CN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern WORD = Pattern.compile("[A-Za-z]+");
    private static final Pattern NUM = Pattern.compile("\\d+");

    /**
     * 中文字数统计口径：中文字符 + 英文单词 + 数字串
     * 与前端 countWords 保持一致，否则前后端字数校验会对不上
     */
    public static int countWords(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int n = 0;
        Matcher cn = CN.matcher(text);
        while (cn.find()) n++;
        Matcher w = WORD.matcher(text);
        while (w.find()) n++;
        Matcher d = NUM.matcher(text);
        while (d.find()) n++;
        return n;
    }

    /** 按句子粗略切分，用于流式吐字时保持标点完整 */
    public static List<String> splitChunks(String text, int minSize, int maxSize) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            buf.append(text.charAt(i));
            char c = text.charAt(i);
            boolean punctuation = "。！？；：”“，、…—\n".indexOf(c) >= 0;
            if (buf.length() >= minSize && punctuation) {
                out.add(buf.toString());
                buf.setLength(0);
            } else if (buf.length() >= maxSize) {
                out.add(buf.toString());
                buf.setLength(0);
            }
        }
        if (buf.length() > 0) {
            out.add(buf.toString());
        }
        return out;
    }

    /** TXT 总纲解析：按「第X卷」切分出卷标题 */
    @Getter
    @Setter
    public static class ParsedOutline {
        private String mainOutline;
        private List<String> volumeTitles = new ArrayList<>();
    }

    private static final Pattern VOLUME =
            Pattern.compile("^\\s*[【\\[]?(第[一二三四五六七八九十百\\d]+卷)[】\\]]?[:：]?\\s*(.*)$");

    /**
     * 解析 TXT 总纲：整份内容作为总纲落库，识别到的卷标题单独返回
     */
    public static ParsedOutline parseTxtOutline(String raw) {
        ParsedOutline po = new ParsedOutline();
        po.setMainOutline(raw);
        if (raw == null) {
            return po;
        }
        for (String line : raw.split("\\r?\\n")) {
            Matcher m = VOLUME.matcher(line.trim());
            if (m.find()) {
                String title = (m.group(1) + " · " + m.group(2)).trim();
                if (title.endsWith("·")) {
                    title = title.substring(0, title.length() - 1).trim();
                }
                po.getVolumeTitles().add(title);
            }
        }
        return po;
    }
}
