package com.wjh.aiwrite.generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 生成任务（文档 §5：generation_jobs）
 *
 * 【MySQL 适配重点】
 * 规则⑦「一章仅允许一个活动生成任务」，PostgreSQL 用 partial unique index 实现：
 *     CREATE UNIQUE INDEX uk_active ON generation_jobs(chapter_id)
 *         WHERE status IN ('QUEUED','RUNNING');
 * MySQL 不支持 partial index，改用虚拟生成列 + 唯一索引（见 schema.sql）：
 *     active_key BIGINT AS (CASE WHEN status IN (...) THEN chapter_id END) VIRTUAL
 *     UNIQUE KEY uk_active_chapter(active_key)
 * MySQL 唯一索引允许多个 NULL，因此效果完全等价，且由数据库强制而非应用层自觉。
 */
@Entity
@Table(name = "generation_jobs")
@Getter
@Setter
public class GenerationJob {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    /** QUEUED / RUNNING / STOP_REQUESTED / STOPPED / FAILED / COMPLETED */
    @Column(length = 24, nullable = false)
    private String status = "QUEUED";

    @Column(name = "current_segment")
    private Integer currentSegment = 0;

    @Column(name = "total_segments")
    private Integer totalSegments = 5;

    @Column(length = 64)
    private String model = "";

    @Column(name = "prompt_version", length = 16)
    private String promptVersion = "v1";

    /** 幂等键：重复点击「生成本章」不会产生第二个任务 */
    @Column(name = "idempotency_key", length = 64, unique = true)
    private String idempotencyKey;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "QUEUED".equals(status) || "RUNNING".equals(status) || "STOP_REQUESTED".equals(status);
    }
}
