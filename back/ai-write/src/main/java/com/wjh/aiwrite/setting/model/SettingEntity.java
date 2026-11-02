package com.wjh.aiwrite.setting.model;

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
import java.util.Map;

/**
 * 设定实体卡（文档 §5：setting_entities）
 *
 * 角色 / 地点 / 组织 / 物品。
 * source 区分为 AI 初稿与人工录入；只有 approvalStatus=CONFIRMED 才进上下文包。
 * revealChapter 用于防止「后续剧情真相」提前泄露到当前章节可见知识中（规则⑥）。
 */
@Entity
@Table(name = "setting_entities")
@Getter
@Setter
public class SettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    /** CHARACTER / PLACE / ORGANIZATION / ITEM */
    @Column(name = "entity_type", length = 24, nullable = false)
    private String entityType;

    @Column(length = 64, nullable = false)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aliases_json")
    private List<String> aliases = new ArrayList<>();

    /** 自由字段：身份 / 性格 / 外貌 / 当前等级 … */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "card_json")
    private Map<String, String> card = new java.util.LinkedHashMap<>();

    /** AI / MANUAL */
    @Column(length = 8)
    private String source = "AI";

    @Column(name = "approval_status", length = 16)
    private String approvalStatus = "DRAFT";

    @Column(name = "reveal_chapter")
    private Integer revealChapter;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
