package com.wjh.aiwrite.setting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 摘要（文档 §5：summaries）
 *
 * 不可妥协规则③：只有 approvalStatus=CONFIRMED 的摘要才进入后续上下文。
 * sourceContentVersion 用于在入库时核对草稿是否基于最新正文生成。
 */
@Entity
@Table(name = "summaries")
@Getter
@Setter
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    /** BOOK / VOLUME / CHAPTER */
    @Column(length = 16)
    private String scope;

    @Column(name = "ref_id")
    private Long refId;

    @Lob
    private String content;

    @Column(name = "approval_status", length = 16)
    private String approvalStatus = "DRAFT";

    @Column(name = "source_content_version")
    private Integer sourceContentVersion = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
