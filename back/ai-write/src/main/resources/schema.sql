-- =====================================================================
-- AI 长篇小说创作台 —— MySQL 8 建表脚本
-- 对应《设计文档 NEW.md》第 5 节「数据库设计」
-- ---------------------------------------------------------------------
-- MySQL 相比 PostgreSQL 的三处适配（其余表结构一一对应）：
--   1) JSONB -> JSON：整存整取，不依赖 GIN 索引
--   2) partial unique index -> 虚拟生成列 + 唯一索引（见 generation_jobs）
--      规则⑦「一章仅允许一个活动任务」依然由数据库强制，不靠应用层自觉
--   3) UUID -> CHAR(36)
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------- 账号 ----------------------------
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    email            VARCHAR(128) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    nickname         VARCHAR(64)  NOT NULL DEFAULT '',
    plan             VARCHAR(16)  NOT NULL DEFAULT 'FREE',
    monthly_quota_chars INT       NOT NULL DEFAULT 120000,
    used_chars       INT          NOT NULL DEFAULT 0,
    created_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ---------------------------- 作品 ----------------------------
CREATE TABLE IF NOT EXISTS novels (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(128) NOT NULL,
    intro         VARCHAR(512) NOT NULL DEFAULT '',
    main_outline  LONGTEXT,
    status        VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    version       INT          NOT NULL DEFAULT 1,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_novels_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作品';

-- ---------------------------- 卷 ----------------------------
CREATE TABLE IF NOT EXISTS volumes (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id       BIGINT       NOT NULL,
    volume_no      INT          NOT NULL,
    title          VARCHAR(128) NOT NULL,
    events_json    JSON,
    status         VARCHAR(24)  NOT NULL DEFAULT 'RAW',
    target_chapters INT         NOT NULL DEFAULT 18,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_volumes_novel_no (novel_id, volume_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='卷（阶段）';

-- ---------------------------- 章纲 ----------------------------
CREATE TABLE IF NOT EXISTS chapter_outlines (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    volume_id      BIGINT       NOT NULL,
    chapter_no     INT          NOT NULL,
    title          VARCHAR(128) NOT NULL DEFAULT '',
    beats_json     JSON         COMMENT '3—7 个 beat',
    characters_json JSON        COMMENT '出场角色名列表',
    estimated_words INT         NOT NULL DEFAULT 2500,
    status         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    version        INT          NOT NULL DEFAULT 1,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_outlines_volume_no (volume_id, chapter_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章纲';

-- ---------------------------- 章节 ----------------------------
CREATE TABLE IF NOT EXISTS chapters (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id        BIGINT       NOT NULL,
    volume_id       BIGINT       NOT NULL,
    outline_id      BIGINT,
    chapter_no      INT          NOT NULL,
    title           VARCHAR(128) NOT NULL DEFAULT '',
    content_md      LONGTEXT     COMMENT '正文唯一权威版本',
    word_count      INT          NOT NULL DEFAULT 0,
    chapter_status  VARCHAR(24)  NOT NULL DEFAULT 'OUTLINED',
    audit_status    VARCHAR(24)  NOT NULL DEFAULT 'NOT_RUN',
    content_version INT          NOT NULL DEFAULT 1 COMMENT '乐观锁',
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_chapters_novel_no (novel_id, chapter_no),
    KEY idx_chapters_volume (volume_id),
    KEY idx_chapters_status (novel_id, chapter_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章节';

CREATE TABLE IF NOT EXISTS chapter_versions (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id   BIGINT       NOT NULL,
    version_no   INT          NOT NULL,
    content_md   LONGTEXT,
    editor_type  VARCHAR(16)  NOT NULL DEFAULT 'HUMAN',
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_versions_chapter_no (chapter_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章节版本历史';

-- ---------------------------- 设定库 ----------------------------
CREATE TABLE IF NOT EXISTS setting_entities (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id       BIGINT       NOT NULL,
    entity_type    VARCHAR(24)  NOT NULL COMMENT 'CHARACTER/PLACE/ORGANIZATION/ITEM',
    name           VARCHAR(64)  NOT NULL,
    aliases_json   JSON,
    card_json      JSON         COMMENT '自由字段：身份/性格/能力…',
    source         VARCHAR(8)   NOT NULL DEFAULT 'AI' COMMENT 'AI/MANUAL',
    approval_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    reveal_chapter INT          NULL COMMENT '首次揭露章号，防止提前泄露',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_entities_novel (novel_id, entity_type),
    UNIQUE KEY uk_entities_novel_name (novel_id, entity_type, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设定实体卡';

CREATE TABLE IF NOT EXISTS world_settings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id       BIGINT       NOT NULL,
    category       VARCHAR(32)  NOT NULL DEFAULT '世界规则',
    title          VARCHAR(128) NOT NULL DEFAULT '',
    content        LONGTEXT,
    source         VARCHAR(8)   NOT NULL DEFAULT 'AI',
    approval_status VARCHAR(16) NOT NULL DEFAULT 'CONFIRMED',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_world_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='世界观';

CREATE TABLE IF NOT EXISTS timeline_events (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id       BIGINT       NOT NULL,
    chapter_id     BIGINT,
    time_label     VARCHAR(128) NOT NULL DEFAULT '',
    event          VARCHAR(512) NOT NULL DEFAULT '',
    approval_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    sort_no        INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_timeline_novel (novel_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间线';

CREATE TABLE IF NOT EXISTS foreshadowings (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id       BIGINT       NOT NULL,
    content        VARCHAR(512) NOT NULL DEFAULT '',
    set_chapter_id BIGINT,
    payoff_chapter_id BIGINT,
    status         VARCHAR(16)  NOT NULL DEFAULT 'PLANTED',
    source         VARCHAR(8)   NOT NULL DEFAULT 'AI',
    approval_status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_foreshadow_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='伏笔';

CREATE TABLE IF NOT EXISTS summaries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id        BIGINT       NOT NULL,
    scope           VARCHAR(16)  NOT NULL COMMENT 'BOOK/VOLUME/CHAPTER',
    ref_id          BIGINT       NOT NULL DEFAULT 0,
    content         LONGTEXT,
    approval_status VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    source_content_version INT   NOT NULL DEFAULT 0,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_summaries (novel_id, scope, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摘要（仅 CONFIRMED 进上下文）';

CREATE TABLE IF NOT EXISTS style_profiles (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id     BIGINT       NOT NULL,
    card_json    JSON,
    sample_text  LONGTEXT,
    forbidden_json JSON,
    created_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_style_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文风卡';

-- ---------------------------- 生成任务 ----------------------------
-- 【MySQL 替代 PostgreSQL partial unique index 的关键】
-- 规则⑦：一章同一时刻只能有一个 QUEUED / RUNNING 的任务。
-- PostgreSQL 写法：CREATE UNIQUE INDEX .. WHERE status IN ('QUEUED','RUNNING');
-- MySQL 无 partial index，改用虚拟生成列 + 唯一索引：
--   活动任务 -> active_key = chapter_id；非活动任务 -> NULL（唯一索引允许多个 NULL）
CREATE TABLE IF NOT EXISTS generation_jobs (
    id              CHAR(36)     NOT NULL PRIMARY KEY COMMENT 'UUID',
    chapter_id      BIGINT       NOT NULL,
    status          VARCHAR(24)  NOT NULL DEFAULT 'QUEUED',
    active_key      BIGINT AS (CASE WHEN status IN ('QUEUED','RUNNING','STOP_REQUESTED')
                                    THEN chapter_id ELSE NULL END) VIRTUAL,
    current_segment INT          NOT NULL DEFAULT 0,
    total_segments  INT          NOT NULL DEFAULT 5,
    model           VARCHAR(64)  NOT NULL DEFAULT '',
    prompt_version  VARCHAR(16)  NOT NULL DEFAULT 'v1',
    idempotency_key VARCHAR(64)  NULL,
    error_code      VARCHAR(64)  NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_active_chapter (active_key),
    UNIQUE KEY uk_idempotency (idempotency_key),
    KEY idx_jobs_chapter (chapter_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成任务（生成列保证单章单活跃任务）';

CREATE TABLE IF NOT EXISTS generation_segments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id      CHAR(36)     NOT NULL,
    segment_no  INT          NOT NULL,
    plan_json   JSON         COMMENT '本段 beats + 目标字数',
    content     LONGTEXT,
    status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_segments_job_no (job_id, segment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分段草稿';

CREATE TABLE IF NOT EXISTS generation_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id      CHAR(36)     NOT NULL,
    event_seq   BIGINT       NOT NULL COMMENT '单调递增，支持 Last-Event-ID 断点续读',
    event_type  VARCHAR(32)  NOT NULL,
    payload_json JSON,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_events_job_seq (job_id, event_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SSE 事件流持久化';

CREATE TABLE IF NOT EXISTS context_snapshots (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id         CHAR(36)     NOT NULL,
    segment_no     INT          NOT NULL,
    context_json   JSON,
    prompt_hash    CHAR(64)     NOT NULL DEFAULT '',
    token_estimate INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_snapshots_job (job_id, segment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上下文快照，便于复现';

-- ---------------------------- 质量闭环 ----------------------------
CREATE TABLE IF NOT EXISTS audit_reports (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id            BIGINT       NOT NULL,
    source_content_version INT         NOT NULL DEFAULT 0,
    issues_json           JSON,
    status                VARCHAR(24)  NOT NULL DEFAULT 'NOT_RUN',
    created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_audit_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='一致性自检报告';

CREATE TABLE IF NOT EXISTS setting_change_drafts (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    chapter_id            BIGINT       NOT NULL,
    source_content_version INT         NOT NULL DEFAULT 0,
    changes_json          JSON,
    summary               LONGTEXT,
    status                VARCHAR(24)  NOT NULL DEFAULT 'DRAFT',
    created_at            DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_change_chapter (chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='摘要与设定变更草稿';

-- ---------------------------- 用量与日志 ----------------------------
CREATE TABLE IF NOT EXISTS usage_records (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id         CHAR(36)     NOT NULL DEFAULT '',
    scene          VARCHAR(32)  NOT NULL DEFAULT '',
    model          VARCHAR(64)  NOT NULL DEFAULT '',
    input_tokens   INT          NOT NULL DEFAULT 0,
    output_tokens  INT          NOT NULL DEFAULT 0,
    estimated_cost DECIMAL(10,4) NOT NULL DEFAULT 0,
    created_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_usage_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用量记录';

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL DEFAULT 0,
    action      VARCHAR(64)  NOT NULL DEFAULT '',
    target_type VARCHAR(32)  NOT NULL DEFAULT '',
    target_id   BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_auditlog (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ---------------------------- 初始账号 ----------------------------
-- 密码 123456 的 BCrypt（强度 10），仅用于本地开发，上线务必修改
INSERT INTO users (email, password_hash, nickname, plan, monthly_quota_chars)
SELECT 'author@ai-write.dev',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
       '墨白', 'PRO', 120000
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'author@ai-write.dev');
