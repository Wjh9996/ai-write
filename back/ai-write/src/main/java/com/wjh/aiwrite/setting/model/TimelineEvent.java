package com.wjh.aiwrite.setting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 时间线事件（文档 §5：timeline_events） */
@Entity
@Table(name = "timeline_events")
@Getter
@Setter
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "time_label", length = 128)
    private String timeLabel = "";

    @Column(length = 512)
    private String event = "";

    @Column(name = "approval_status", length = 16)
    private String approvalStatus = "DRAFT";

    @Column(name = "sort_no")
    private Integer sortNo = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
