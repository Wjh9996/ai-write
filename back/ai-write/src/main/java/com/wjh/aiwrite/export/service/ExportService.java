package com.wjh.aiwrite.export.service;

import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.novel.repository.NovelRepository;
import com.wjh.aiwrite.outline.model.Volume;
import com.wjh.aiwrite.outline.repository.VolumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 导出服务（Model 层业务）—— 文档 §6：GET /api/novels/{id}/export?format=docx
 *
 * 保留卷 / 章标题层级（文档 §设计思考 第 7 问）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final NovelRepository novelRepository;
    private final VolumeRepository volumeRepository;
    private final ChapterRepository chapterRepository;

    public record ExportedFile(String fileName, byte[] bytes, String contentType) {
    }

    public ExportedFile export(Long novelId, String scope, String format,
                               boolean withTitle, boolean withAuthor) {
        var novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("作品不存在"));

        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNo(novelId);
        List<Volume> volumes = volumeRepository.findByNovelIdOrderByVolumeNo(novelId);

        if ("docx".equalsIgnoreCase(format)) {
            byte[] bytes = buildDocx(novel.getTitle(), chapters, volumes, withTitle, withAuthor);
            return new ExportedFile(novel.getTitle() + ".docx", bytes,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }

        // md / txt 走纯文本，标题用 # 或空行区分
        StringBuilder sb = new StringBuilder();
        if (withTitle) {
            sb.append("# ").append(novel.getTitle()).append("\n\n");
        }
        Long lastVolume = null;
        for (Chapter c : chapters) {
            if (withTitle && !c.getVolumeId().equals(lastVolume)) {
                lastVolume = c.getVolumeId();
                final long volumeId = c.getVolumeId();
                volumes.stream().filter(v -> v.getId().equals(volumeId)).findFirst()
                        .ifPresent(v -> sb.append("\n## ").append(v.getTitle()).append("\n\n"));
            }
            if (withTitle) {
                sb.append("### ").append(c.getTitle()).append("\n\n");
            }
            sb.append(c.getContentMd() == null ? "" : c.getContentMd()).append("\n\n");
        }
        String ext = "md".equalsIgnoreCase(format) ? "md" : "txt";
        return new ExportedFile(novel.getTitle() + "." + ext, sb.toString().getBytes(),
                "text/plain; charset=UTF-8");
    }

    private byte[] buildDocx(String title, List<Chapter> chapters, List<Volume> volumes,
                             boolean withTitle, boolean withAuthor) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            if (withTitle) {
                XWPFParagraph p = doc.createParagraph();
                p.setAlignment(ParagraphAlignment.CENTER);
                var r = p.createRun();
                r.setBold(true);
                r.setFontSize(22);
                r.setText(title);
            }
            if (withAuthor) {
                XWPFParagraph p = doc.createParagraph();
                p.setAlignment(ParagraphAlignment.CENTER);
                var r = p.createRun();
                r.setFontSize(11);
                r.setText("由「长篇小说 AI 创作台」导出");
            }

            Long lastVolume = null;
            for (Chapter c : chapters) {
                if (withTitle && !c.getVolumeId().equals(lastVolume)) {
                    lastVolume = c.getVolumeId();
                    final long volumeId = c.getVolumeId();
                    Volume v = volumes.stream().filter(x -> x.getId().equals(volumeId))
                            .findFirst().orElse(null);
                    if (v != null) {
                        XWPFParagraph p = doc.createParagraph();
                        p.setStyle("Heading1");
                        var r = p.createRun();
                        r.setBold(true);
                        r.setFontSize(18);
                        r.setText(v.getTitle());
                    }
                }
                if (withTitle) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setStyle("Heading2");
                    var r = p.createRun();
                    r.setBold(true);
                    r.setFontSize(15);
                    r.setText(c.getTitle());
                }
                String body = c.getContentMd() == null ? "" : c.getContentMd();
                for (String para : body.split("\\n{2,}")) {
                    String text = para.trim();
                    if (text.isEmpty() || text.startsWith("#")) {
                        continue;
                    }
                    XWPFParagraph p = doc.createParagraph();
                    var r = p.createRun();
                    r.setFontSize(12);
                    r.setText(text);
                }
            }
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[export] docx 生成失败", e);
            throw new IllegalStateException("导出失败：" + e.getMessage());
        }
    }
}
