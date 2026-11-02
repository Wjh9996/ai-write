package com.wjh.aiwrite.chapter.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 章节版本历史（文档 §5：chapter_versions） */
@Entity
@Table(name = "chapter_versions")
@Getter
@Setter
public class ChapterVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Lob
    @Column(name = "content_md")
    private String contentMd;

    /** AI / HUMAN */
    @Column(name = "editor_type", length = 16)
    private String editorType = "HUMAN";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
