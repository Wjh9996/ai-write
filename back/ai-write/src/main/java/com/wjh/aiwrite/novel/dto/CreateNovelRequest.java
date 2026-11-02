package com.wjh.aiwrite.novel.dto;

import jakarta.validation.constraints.NotBlank;

/** 新建作品请求体 */
public record CreateNovelRequest(
        @NotBlank(message = "作品名不能为空") String title,
        String intro,
        String mainOutline
) {
}
