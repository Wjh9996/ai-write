package com.wjh.aiwrite.novel.controller;

import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.novel.dto.CreateNovelRequest;
import com.wjh.aiwrite.novel.dto.NovelDto;
import com.wjh.aiwrite.novel.service.NovelService;
import com.wjh.aiwrite.support.Ownership;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 作品 Controller（View 层）—— 文档 §6：POST/GET /api/novels、PUT outline、POST import
 *
 * 只做：参数校验 → 权限校验 → 调 Service → 包 ApiResult
 */
@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
public class NovelController {

    private final NovelService novelService;
    private final Ownership ownership;

    @GetMapping
    public ApiResult<List<NovelDto>> list() {
        return ApiResult.ok(novelService.list(ownership.userId()));
    }

    @GetMapping("/{id}")
    public ApiResult<NovelDto> detail(@PathVariable Long id) {
        ownership.requireNovel(id);
        return ApiResult.ok(novelService.detail(id));
    }

    @PostMapping
    public ApiResult<NovelDto> create(@Valid @RequestBody CreateNovelRequest req) {
        return ApiResult.ok(novelService.create(ownership.userId(), req), "创建成功");
    }

    @PutMapping("/{id}/outline")
    public ApiResult<NovelDto> updateOutline(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ownership.requireNovel(id);
        return ApiResult.ok(novelService.updateOutline(id, body.getOrDefault("mainOutline", "")), "总纲已保存");
    }

    @PostMapping("/{id}/outline/import")
    public ApiResult<NovelDto> importTxt(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        ownership.requireNovel(id);
        validateTxt(file);
        return ApiResult.ok(novelService.importTxt(id, file), "导入成功");
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> remove(@PathVariable Long id) {
        ownership.requireNovel(id);
        novelService.remove(id);
        return ApiResult.ok(null, "已删除");
    }

    /** 文档 §9：上传需校验大小与文件类型 */
    private void validateTxt(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件为空");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TXT 不能超过 2MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".txt")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "首版只支持 TXT 大纲");
        }
    }
}
