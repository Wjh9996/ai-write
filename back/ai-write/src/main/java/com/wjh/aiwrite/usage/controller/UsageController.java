package com.wjh.aiwrite.usage.controller;

import com.wjh.aiwrite.common.ApiResult;
import com.wjh.aiwrite.usage.dto.UsageOverviewDto;
import com.wjh.aiwrite.usage.service.UsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 用量 Controller（View 层）—— 文档 §6：GET /api/usage */
@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;

    @GetMapping
    public ApiResult<UsageOverviewDto> overview() {
        return ApiResult.ok(usageService.overview());
    }
}
