package com.wjh.aiwrite.generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 分段草稿（文档 §5：generation_segments）
 * 停止时只取消未开始的后续段，已生成内容保留（文档 §4.3）
 */
@Entity
@Table(name = "generation_segments")
@Getter
@Setter
public class GenerationSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", length = 36, nullable = false)
    private String jobId;

    @Column(name = "segment_no", nullable = false)
    private Integer segmentNo;

    /** 本段目标字数 */
    @Column(name = "plan_json")
    private String planJson;

    @Lob
    private String content;

    /** PENDING / RUNNING / COMPLETED / INCOMPLETE / FAILED */
    @Column(length = 16)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
