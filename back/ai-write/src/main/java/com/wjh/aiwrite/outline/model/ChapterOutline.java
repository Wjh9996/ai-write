package com.wjh.aiwrite.outline.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 章纲（文档 §5：chapter_outlines）
 *
 * 不可妥协规则①：只有 status=CONFIRMED 的章纲才允许生成正文
 */
@Entity
@Table(name = "chapter_outlines")
@Getter
@Setter
public class ChapterOutline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "volume_id", nullable = false)
    private Long volumeId;

    @Column(name = "chapter_no", nullable = false)
    private Integer chapterNo;

    @Column(length = 128)
    private String title = "";

    /** 3—7 个 beat，业务层强制校验 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "beats_json")
    private List<String> beats = new ArrayList<>();

    /** 出场角色名，必须是设定库里已有的实体 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "characters_json")
    private List<String> characters = new ArrayList<>();

    @Column(name = "estimated_words")
    private Integer estimatedWords = 2500;

    /** DRAFT / CONFIRMED */
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
