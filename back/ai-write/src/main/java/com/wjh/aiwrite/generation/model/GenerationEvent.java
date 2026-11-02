package com.wjh.aiwrite.generation.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SSE 事件持久化（文档 §5：generation_events）
 *
 * 用途：SSE 断线不等于任务停止，客户端可用 Last-Event-ID 补读历史事件（文档 §4.3）
 */
@Entity
@Table(name = "generation_events")
@Getter
@Setter
public class GenerationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", length = 36, nullable = false)
    private String jobId;

    /** 单调递增事件序号，即 SSE 的 event id */
    @Column(name = "event_seq", nullable = false)
    private Long eventSeq;

    /** job.started / segment.started / text.delta / segment.completed / chapter.draft_ready / ... */
    @Column(name = "event_type", length = 32, nullable = false)
    private String eventType;

    /** 注意：payload 中禁止包含 API Key 与完整系统提示词（文档 §6） */
    @Column(name = "payload_json")
    private String payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
