package com.wjh.aiwrite.review.dto;

import java.util.List;

/** 人工确认入库请求 */
public record ApproveRequest(String summary, List<ChangeDto> changes, String notes) {
}
