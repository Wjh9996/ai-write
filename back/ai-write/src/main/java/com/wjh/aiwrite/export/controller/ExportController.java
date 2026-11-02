package com.wjh.aiwrite.export.controller;

import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.export.service.ExportService;
import com.wjh.aiwrite.support.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导出 Controller（View 层）—— 文档 §6：GET /api/novels/{id}/export?format=docx
 *
 * docx 直接返回二进制流；md / txt 也走同一个入口。
 * 导出是操作留痕，不改变章节状态（文档 §4.3）。
 */
@RestController
@RequestMapping("/api/novels/{novelId}/export")
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;
    private final Ownership ownership;

    @GetMapping
    public ResponseEntity<?> export(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "docx") String format,
            @RequestParam(defaultValue = "BOOK") String scope,
            @RequestParam(defaultValue = "true") boolean withTitle,
            @RequestParam(defaultValue = "false") boolean withAuthor) {

        ownership.requireNovel(novelId);
        ExportService.ExportedFile file = exportService.export(novelId, scope, format, withTitle, withAuthor);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.fileName() + "\"")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    /** 前端「导出页」需要一次 JSON 形式的结果（展示文件名与大小） */
    @GetMapping("/meta")
    public ApiResult<ExportMeta> meta(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "docx") String format,
            @RequestParam(defaultValue = "BOOK") String scope,
            @RequestParam(defaultValue = "true") boolean withTitle,
            @RequestParam(defaultValue = "false") boolean withAuthor) {

        ownership.requireNovel(novelId);
        ExportService.ExportedFile file = exportService.export(novelId, scope, format, withTitle, withAuthor);
        return ApiResult.ok(new ExportMeta(file.fileName(), file.bytes().length / 1024, scope, format));
    }

    public record ExportMeta(String fileName, Integer sizeKb, String scope, String format) {
    }
}
