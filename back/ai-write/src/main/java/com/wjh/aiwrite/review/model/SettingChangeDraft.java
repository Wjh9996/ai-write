package com.wjh.aiwrite.review.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 摘要与设定变更草稿（文档 §5：setting_change_drafts）
 *
 * 规则③：草稿只有在人工点击「通过并入库」后，才在同一事务里写回摘要 / 角色状态 / 时间线 / 伏笔。
 */
@Entity
@Table(name = "setting_change_drafts")
@Getter
@Setter
public class SettingChangeDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_id", nullable = false)
    private Long chapterId;

    @Column(name = "source_content_version")
    private Integer sourceContentVersion = 0;

    /** JSON 数组：{id,kind,name,change,time,action,entityType,accepted} */
    @Lob
    @Column(name = "changes_json")
    private String changesJson;

    @Lob
    private String summary;

    /** DRAFT / APPROVED / REJECTED */
    @Column(length = 24)
    private String status = "DRAFT";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
