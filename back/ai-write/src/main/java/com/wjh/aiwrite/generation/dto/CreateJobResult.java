package com.wjh.aiwrite.generation.dto;

/** 创建生成任务返回体（文档 §6：异步任务创建返回 202 + jobId） */
public record CreateJobResult(String jobId, String status, Integer currentSegment, Integer totalSegments) {
}
