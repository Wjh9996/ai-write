package com.wjh.aiwrite.outline.controller;

import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.outline.dto.GenerateOutlinesResult;
import com.wjh.aiwrite.outline.dto.OutlineDto;
import com.wjh.aiwrite.outline.dto.VolumeDto;
import com.wjh.aiwrite.outline.service.OutlineService;
import com.wjh.aiwrite.support.Ownership;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 大纲 Controller（View 层）—— 文档 §6：卷 CRUD、AI 拆本卷章纲、批量确认
 *
 * 只做 HTTP 与校验，业务规则在 OutlineService。
 */
@RestController
@RequiredArgsConstructor
public class OutlineController {

    private final OutlineService outlineService;
    private final Ownership ownership;

    // ---------------- 卷 ----------------

    @GetMapping("/api/novels/{novelId}/volumes")
    public ApiResult<List<VolumeDto>> volumes(@PathVariable Long novelId) {
        ownership.requireNovel(novelId);
        return ApiResult.ok(outlineService.volumes(novelId));
    }

    @PostMapping("/api/novels/{novelId}/volumes")
    public ApiResult<VolumeDto> createVolume(@PathVariable Long novelId,
                                             @RequestBody Map<String, String> body) {
        ownership.requireNovel(novelId);
        return ApiResult.ok(outlineService.createVolume(novelId, body.get("title")), "已新增卷");
    }

    @PutMapping("/api/volumes/{volumeId}/events")
    public ApiResult<VolumeDto> saveEvents(@PathVariable Long volumeId,
                                           @RequestBody Map<String, List<String>> body) {
        requireVolume(volumeId);
        return ApiResult.ok(outlineService.saveEvents(volumeId, body.get("events")), "阶段事件已保存");
    }

    // ---------------- 章纲 ----------------

    @GetMapping("/api/volumes/{volumeId}/outlines")
    public ApiResult<List<OutlineDto>> outlines(@PathVariable Long volumeId) {
        requireVolume(volumeId);
        return ApiResult.ok(outlineService.outlines(volumeId));
    }

    @PostMapping("/api/volumes/{volumeId}/outline-generation")
    public ApiResult<GenerateOutlinesResult> generate(@PathVariable Long volumeId) {
        requireVolume(volumeId);
        return ApiResult.ok(outlineService.generate(volumeId), "章纲已生成");
    }

    /** 确认本卷章纲：确认后章节进入创作列表（状态 OUTLINED） */
    @PutMapping("/api/volumes/{volumeId}/outlines/confirm")
    public ApiResult<VolumeDto> confirm(@PathVariable Long volumeId) {
        requireVolume(volumeId);
        return ApiResult.ok(outlineService.confirm(volumeId), "本卷章纲已确认");
    }

    @PutMapping("/api/outlines/{id}")
    public ApiResult<OutlineDto> update(@PathVariable Long id, @RequestBody OutlineDto patch) {
        requireNovelOfOutline(id);
        return ApiResult.ok(outlineService.update(id, patch), "已保存");
    }

    @DeleteMapping("/api/outlines/{id}")
    public ApiResult<Void> remove(@PathVariable Long id) {
        requireNovelOfOutline(id);
        outlineService.remove(id);
        return ApiResult.ok(null, "已删除");
    }

    @PostMapping("/api/volumes/{volumeId}/outlines")
    public ApiResult<OutlineDto> insert(@PathVariable Long volumeId,
                                        @RequestBody Map<String, Integer> body) {
        requireVolume(volumeId);
        return ApiResult.ok(outlineService.insert(volumeId, body.getOrDefault("afterChapterNo", 0)), "已插入");
    }

    @PutMapping("/api/volumes/{volumeId}/outlines/reorder")
    public ApiResult<Void> reorder(@PathVariable Long volumeId,
                                   @RequestBody List<Map<String, Integer>> orders) {
        requireVolume(volumeId);
        outlineService.reorder(volumeId, orders.stream()
                .map(m -> new long[]{m.get("id"), m.get("chapterNo")})
                .toList());
        return ApiResult.ok(null, "顺序已更新");
    }

    // ---------------- 权限校验 ----------------

    private void requireVolume(Long volumeId) {
        ownership.requireNovel(outlineService.novelIdOf(volumeId));
    }

    private void requireNovelOfOutline(Long outlineId) {
        ownership.requireNovel(outlineService.novelIdOfOutline(outlineId));
    }
}
