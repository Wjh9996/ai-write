package com.wjh.aiwrite.outline.dto;

import java.util.List;

/** AI 拆章纲结果：含「待新增设定」，新实体不默默入库（文档 §4.1） */
public record GenerateOutlinesResult(
        Long volumeId,
        List<OutlineDto> outlines,
        List<ProposedEntityDto> proposedEntities
) {
}
