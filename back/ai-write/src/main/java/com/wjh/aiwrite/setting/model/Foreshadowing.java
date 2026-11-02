package com.wjh.aiwrite.setting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 伏笔（文档 §5：foreshadowings） */
@Entity
@Table(name = "foreshadowings")
@Getter
@Setter
public class Foreshadowing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(length = 512)
    private String content = "";

    @Column(name = "set_chapter_id")
    private Long setChapterId;

    @Column(name = "payoff_chapter_id")
    private Long payoffChapterId;

    /** PLANTED / PAID_OFF */
    @Column(length = 16)
    private String status = "PLANTED";

    @Column(length = 8)
    private String source = "AI";

    @Column(name = "approval_status", length = 16)
    private String approvalStatus = "DRAFT";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
