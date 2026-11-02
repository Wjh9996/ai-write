package com.wjh.aiwrite.chapter.dto;

/** 保存正文请求：带 expectedVersion 做乐观锁 */
public record SaveContentRequest(String contentMd, Integer expectedVersion) {
}
