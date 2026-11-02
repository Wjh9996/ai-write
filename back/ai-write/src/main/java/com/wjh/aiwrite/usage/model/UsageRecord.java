package com.wjh.aiwrite.usage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用量记录（文档 §5：usage_records）
 *
 * 每次模型调用都要记录实际 tokens、模型与估算费用（文档 §9）。
 * 估算价格来自配置，需要按真实账单校准。
 */
@Entity
@Table(name = "usage_records")
@Getter
@Setter
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", length = 36)
    private String jobId = "";

    /** 拆章纲 / 分段生成 / 一致性自检 / 摘要草稿 / 局部重写 */
    @Column(length = 32)
    private String scene = "";

    @Column(length = 64)
    private String model = "";

    @Column(name = "input_tokens")
    private Integer inputTokens = 0;

    @Column(name = "output_tokens")
    private Integer outputTokens = 0;

    @Column(name = "estimated_cost", precision = 10, scale = 4)
    private BigDecimal estimatedCost = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
