package com.wjh.aiwrite.chapter.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 章节（文档 §5：chapters）
 *
 * 不可妥协规则④：content_md 是正文唯一的当前权威版本
 * 乐观锁：content_version，禁止 AI 覆盖人工编辑（规则⑤）
 */
@Entity
@Table(name = "chapters")
@Getter
@Setter
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "volume_id", nullable = false)
    private Long volumeId;

    @Column(name = "outline_id")
    private Long outlineId;

    @Column(name = "chapter_no", nullable = false)
    private Integer chapterNo;

    @Column(length = 128)
    private String title = "";

    @Lob
    @Column(name = "content_md")
    private String contentMd;

    @Column(name = "word_count")
    private Integer wordCount = 0;

    /** OUTLINED / GENERATING / REVIEW_PENDING / APPROVED */
    @Column(name = "chapter_status", length = 24)
    private String chapterStatus = "OUTLINED";

    /** NOT_RUN / DRAFT / PASSED / NEEDS_REVIEW */
    @Column(name = "audit_status", length = 24)
    private String auditStatus = "NOT_RUN";

    /** 乐观锁版本号 */
    @Column(name = "content_version")
    private Integer contentVersion = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
