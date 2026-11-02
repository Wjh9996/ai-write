package com.wjh.aiwrite.setting.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/** 世界观（文档 §5：world_settings） */
@Entity
@Table(name = "world_settings")
@Getter
@Setter
public class WorldSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(length = 32)
    private String category = "世界规则";

    @Column(length = 128)
    private String title = "";

    @Lob
    private String content;

    @Column(length = 8)
    private String source = "AI";

    @Column(name = "approval_status", length = 16)
    private String approvalStatus = "CONFIRMED";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
