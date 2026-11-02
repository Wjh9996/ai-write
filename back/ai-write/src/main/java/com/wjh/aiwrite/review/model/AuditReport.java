package com.wjh.aiwrite.review.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 一致性自检报告（文档 §5：audit_reports）
 *
 * 文档 §4.4：模型自检只提出带正文引文的 issue，不宣称零幻觉；
 * 规则校验与模型自检结合，新增设定一律形成待确认提案。
 */
@Entity
@Table(name = "audit_reports")
@Getter
@Setter
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "source_content_version")
    private Integer sourceContentVersion = 0;

    /** JSON 数组：{id,type,level,quote,issue,suggest,resolved} */
    @Lob
    @Column(name = "issues_json")
    private String issuesJson;

    /** NOT_RUN / DRAFT / PASSED / NEEDS_REVIEW */
    @Column(length = 24)
    private String status = "NOT_RUN";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
