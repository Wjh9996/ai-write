package com.wjh.aiwrite.setting.controller;

import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.novel.repository.NovelRepository;
import com.wjh.aiwrite.setting.dto.EntityDto;
import com.wjh.aiwrite.setting.dto.NovelSettingsDto;
import com.wjh.aiwrite.setting.dto.StyleDto;
import com.wjh.aiwrite.setting.service.SettingService;
import com.wjh.aiwrite.support.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 设定库 Controller（View 层）—— 文档 §6：GET/PUT /api/novels/{id}/settings
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;
    private final NovelRepository novelRepository;
    private final Ownership ownership;

    @GetMapping("/novels/{novelId}/settings")
    public ApiResult<NovelSettingsDto> get(@PathVariable Long novelId) {
        ownership.requireNovel(novelId);
        return ApiResult.ok(settingService.get(novelId));
    }

    @PutMapping("/novels/{novelId}/settings")
    public ApiResult<NovelSettingsDto> save(@PathVariable Long novelId,
                                            @RequestBody NovelSettingsDto dto) {
        ownership.requireNovel(novelId);
        return ApiResult.ok(settingService.save(novelId, dto), "设定已保存");
    }

    @PostMapping("/novels/{novelId}/settings/extract")
    public ApiResult<NovelSettingsDto> extract(@PathVariable Long novelId) {
        ownership.requireNovel(novelId);
        String outline = novelRepository.findById(novelId)
                .map(n -> n.getMainOutline() == null ? "" : n.getMainOutline())
                .orElse("");
        return ApiResult.ok(settingService.extract(novelId, outline), "AI 初稿已生成，请人工确认");
    }

    @PutMapping("/entities/{id}")
    public ApiResult<EntityDto> saveEntity(@PathVariable Long id, @RequestBody EntityDto dto) {
        ownership.requireNovel(dto.novelId() == null ? 0L : dto.novelId());
        return ApiResult.ok(settingService.saveEntity(dto.novelId(), dto), "设定已保存");
    }

    @DeleteMapping("/entities/{id}")
    public ApiResult<Void> removeEntity(@PathVariable Long id) {
        settingService.removeEntity(id);
        return ApiResult.ok(null, "已删除");
    }

    @PutMapping("/novels/{novelId}/style")
    public ApiResult<StyleDto> saveStyle(@PathVariable Long novelId, @RequestBody StyleDto dto) {
        ownership.requireNovel(novelId);
        return ApiResult.ok(settingService.saveStyle(novelId, dto), "文风卡已保存");
    }
}
