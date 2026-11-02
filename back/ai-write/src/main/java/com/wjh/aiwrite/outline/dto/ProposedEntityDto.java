package com.wjh.aiwrite.outline.dto;

/** AI 提出的「待新增设定」：不直接入库，等人工确认（文档 §4.1） */
public record ProposedEntityDto(String id, String type, String name, String reason) {
}
