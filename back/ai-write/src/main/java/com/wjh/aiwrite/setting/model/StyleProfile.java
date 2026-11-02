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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文风卡（文档 §5：style_profiles）
 * 每个生成段落都会带上它，文风改动即时影响后续正文。
 */
@Entity
@Table(name = "style_profiles")
@Getter
@Setter
public class StyleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    /** 人称 / 视角 / 节奏 / 基调 / 对话比例 / 句长 / 爽点 / 禁忌 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "card_json")
    private Map<String, String> card = new LinkedHashMap<>();

    @Lob
    @Column(name = "sample_text")
    private String sampleText;

    /** 禁用词 / 禁止写法 */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "forbidden_json")
    private List<String> forbidden = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
