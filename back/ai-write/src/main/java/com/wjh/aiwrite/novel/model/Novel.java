package com.wjh.aiwrite.novel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 作品（文档 §5：novels）
 */
@Entity
@Table(name = "novels")
@Getter
@Setter
public class Novel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 512)
    private String intro = "";

    /** 全书总纲：上下文体系的源头 */
    @Lob
    @Column(name = "main_outline")
    private String mainOutline;

    /** DRAFT / WRITING / PAUSED / FINISHED */
    @Column(length = 16)
    private String status = "DRAFT";

    @Version
    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer version = 1;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
