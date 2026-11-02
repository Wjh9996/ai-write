package com.wjh.aiwrite.setting.dto;

import java.util.List;

/** 设定库整体返回（前端 NovelSettings 一致） */
public record NovelSettingsDto(
        List<EntityDto> entities,
        List<WorldDto> worlds,
        List<TimelineDto> timeline,
        List<ForeshadowingDto> foreshadowings,
        StyleDto style
) {
}
