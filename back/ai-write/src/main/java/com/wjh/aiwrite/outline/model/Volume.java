package com.wjh.aiwrite.outline.model;

import com.wjh.aiwrite.novel.model.Novel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 卷（文档 §5：volumes）—— 一个阶段
 */
@Entity
@Table(name = "volumes")
@Getter
@Setter
public class Volume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Column(name = "volume_no", nullable = false)
    private Integer volumeNo;

    @Column(length = 128)
    private String title;

    /**
     * 有序阶段事件（MySQL JSON 列）
     * 文档建议 events_jsonb；MySQL 下用 JSON，整存整取，不需要 JSON 内部索引
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "events_json")
    private List<String> events = new ArrayList<>();

    /** RAW / OUTLINED / PENDING_CONFIRM / CONFIRMED */
    @Column(length = 24)
    private String status = "RAW";

    @Column(name = "target_chapters")
    private Integer targetChapters = 18;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
