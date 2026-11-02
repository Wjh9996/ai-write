package com.wjh.aiwrite.chapter.controller;

import com.wjh.aiwrite.chapter.dto.ChapterDto;
import com.wjh.aiwrite.chapter.dto.ChapterVersionDto;
import com.wjh.aiwrite.chapter.dto.SaveContentRequest;
import com.wjh.aiwrite.chapter.service.ChapterService;
import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.support.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 章节 Controller（View 层）—— 文档 §6：章节列表、正文保存、版本历史
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChapterController {

    private final ChapterService chapterService;
    private final Ownership ownership;

    @GetMapping("/novels/{novelId}/chapters")
    public ApiResult<List<ChapterDto>> list(@PathVariable Long novelId) {
        ownership.requireNovel(novelId);
        return ApiResult.ok(chapterService.list(novelId));
    }

    @GetMapping("/chapters/{id}")
    public ApiResult<ChapterDto> detail(@PathVariable Long id) {
        ownership.requireNovel(chapterService.novelIdOf(id));
        return ApiResult.ok(chapterService.detail(id));
    }

    /** PUT /api/chapters/{id}/content —— 带 expectedVersion 的乐观锁保存 */
    @PutMapping("/chapters/{id}/content")
    public ApiResult<ChapterDto> saveContent(@PathVariable Long id, @RequestBody SaveContentRequest req) {
        ownership.requireNovel(chapterService.novelIdOf(id));
        return ApiResult.ok(chapterService.saveContent(id, req), "已保存");
    }

    @GetMapping("/chapters/{id}/versions")
    public ApiResult<List<ChapterVersionDto>> versions(@PathVariable Long id) {
        ownership.requireNovel(chapterService.novelIdOf(id));
        return ApiResult.ok(chapterService.versions(id));
    }

    /** 前端按钮禁用依据：未确认章纲 / 前一章未审核 / 正在生成 */
    @GetMapping("/chapters/{id}/can-generate")
    public ApiResult<Map<String, Boolean>> canGenerate(@PathVariable Long id) {
        ownership.requireNovel(chapterService.novelIdOf(id));
        return ApiResult.ok(Map.of("ok", chapterService.canGenerate(id)));
    }
}
