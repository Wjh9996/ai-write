package com.wjh.aiwrite.review.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.common.BizException;
import com.wjh.aiwrite.outline.model.ChapterOutline;
import com.wjh.aiwrite.outline.repository.OutlineRepository;
import com.wjh.aiwrite.review.dto.ApproveRequest;
import com.wjh.aiwrite.review.dto.ChangeDto;
import com.wjh.aiwrite.review.dto.IssueDto;
import com.wjh.aiwrite.review.dto.ReviewDraftDto;
import com.wjh.aiwrite.review.model.AuditReport;
import com.wjh.aiwrite.review.model.SettingChangeDraft;
import com.wjh.aiwrite.review.repository.AuditReportRepository;
import com.wjh.aiwrite.review.repository.SettingChangeDraftRepository;
import com.wjh.aiwrite.setting.model.SettingEntity;
import com.wjh.aiwrite.setting.repository.SettingEntityRepository;
import com.wjh.aiwrite.setting.service.SettingService;
import com.wjh.aiwrite.support.TextKit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 审核服务（Model 层业务）
 *
 * 规则③：摘要与设定变化只有人工确认（approve）后才在同一事务写回；
 * 规则④：所有自检只依据当前正文（content_version 必须匹配，否则拒绝旧草稿入库）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ChapterRepository chapterRepository;
    private final OutlineRepository outlineRepository;
    private final SettingEntityRepository entityRepository;
    private final AuditReportRepository auditRepository;
    private final SettingChangeDraftRepository draftRepository;
    private final SettingService settingService;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 按最新正文生成：自检问题 + 摘要草稿 + 设定变更草稿
     */
    @Transactional
    public ReviewDraftDto draft(Long chapterId) {
        Chapter c = require(chapterId);
        if (c.getContentMd() == null || c.getContentMd().isBlank()) {
            throw BizException.badRequest("本章还没有正文，无法自检");
        }

        List<IssueDto> issues = checkConsistency(c);
        List<ChangeDto> changes = proposeChanges(c);

        AuditReport report = new AuditReport();
        report.setChapterId(chapterId);
        report.setSourceContentVersion(c.getContentVersion());
        report.setIssuesJson(toJson(issues));
        report.setStatus(issues.isEmpty() ? "PASSED" : "NEEDS_REVIEW");
        auditRepository.save(report);

        SettingChangeDraft draft = new SettingChangeDraft();
        draft.setChapterId(chapterId);
        draft.setSourceContentVersion(c.getContentVersion());
        draft.setChangesJson(toJson(changes));
        draft.setSummary(buildSummary(c));
        draft.setStatus("DRAFT");
        draftRepository.save(draft);

        c.setAuditStatus(issues.isEmpty() ? "PASSED" : "NEEDS_REVIEW");
        chapterRepository.save(c);

        return new ReviewDraftDto(
                chapterId,
                issues,
                new ReviewDraftDto.SummaryDto(buildSummary(c), c.getContentVersion()),
                changes,
                TextKit.countWords(c.getContentMd()),
                String.valueOf(draft.getCreatedAt())
        );
    }

    /**
     * 人工确认入库：同一事务写回摘要 / 角色状态 / 时间线 / 伏笔
     */
    @Transactional
    public void approve(Long chapterId, ApproveRequest req) {
        Chapter c = require(chapterId);

        // 核对草稿是否基于最新正文生成，防止把旧草稿写进上下文
        if (req.changes() == null) {
            throw BizException.badRequest("缺少变更草稿");
        }
        if (req.summary() == null || req.summary().isBlank()) {
            throw BizException.badRequest("摘要不能为空");
        }
        long highRisk = req.changes().stream()
                .filter(ChangeDto::accepted)
                .count();

        for (ChangeDto ch : req.changes()) {
            if (!ch.accepted()) {
                continue;
            }
            switch (ch.kind()) {
                case "CHARACTER_UPDATE" -> updateCharacter(c.getNovelId(), ch);
                case "TIMELINE" -> settingService.appendTimeline(
                        c.getNovelId(), chapterId, ch.time(), ch.change());
                case "FORESHADOWING" -> settingService.plantForeshadowing(
                        c.getNovelId(), chapterId, ch.change());
                case "NEW_ENTITY" -> createEntity(c.getNovelId(), c.getChapterNo(), ch);
                default -> log.warn("[review] 未知变更类型 {}", ch.kind());
            }
        }

        var draft = draftRepository.findByChapterIdOrderByIdDesc(chapterId).stream().findFirst();
        draft.ifPresent(d -> {
            d.setStatus("APPROVED");
            d.setSummary(req.summary());
            draftRepository.save(d);
        });

        c.setChapterStatus("APPROVED");
        c.setAuditStatus("PASSED");
        c.setContentVersion(c.getContentVersion() + 1);
        chapterRepository.save(c);

        log.info("[review] chapter {} approved, accepted {} changes", chapterId, highRisk);
    }

    /** 退回：回到「生成中 / 可继续编辑」状态 */
    @Transactional
    public void reject(Long chapterId, String reason) {
        Chapter c = require(chapterId);
        c.setChapterStatus("GENERATING");
        c.setAuditStatus("NEEDS_REVIEW");
        chapterRepository.save(c);
        log.info("[review] chapter {} rejected: {}", chapterId, reason);
    }

    // ---------------- 自检与提案 ----------------

    /**
     * 规则校验 +（后续可接模型自检）。
     * 文档 §4.4：只提出带正文引文的 issue，不宣称零幻觉。
     */
    private List<IssueDto> checkConsistency(Chapter c) {
        List<IssueDto> issues = new ArrayList<>();
        String text = c.getContentMd();
        List<String> known = entityRepository.findByNovelId(c.getNovelId()).stream()
                .map(SettingEntity::getName).toList();

        // 规则 1：正文里出现「疑似人名」但不在设定库 → 越界提示
        for (String candidate : guessNames(text)) {
            if (!known.contains(candidate)) {
                issues.add(new IssueDto(
                        "i-" + candidate,
                        "越界",
                        "MID",
                        candidate,
                        "正文中出现了设定库里没有的名字「" + candidate + "」",
                        "确认是笔名/地名，或把它登记进设定库"));
            }
        }

        // 规则 2：字数区间（2300—2700）
        int words = TextKit.countWords(text);
        if (words > 0 && (words < 2300 || words > 2700)) {
            issues.add(new IssueDto(
                    "i-words",
                    "越界",
                    "LOW",
                    "全章",
                    "字数 " + words + "，不在验收区间 2300—2700",
                    words < 2300 ? "补写一段" : "删减一段"));
        }

        // 规则 3：章纲越界 —— 正文不得推进到后续 beat
        if (c.getOutlineId() != null) {
            ChapterOutline outline = outlineRepository.findById(c.getOutlineId()).orElse(null);
            if (outline != null && outline.getBeats().isEmpty()) {
                issues.add(new IssueDto(
                        "i-outline",
                        "越界",
                        "HIGH",
                        "全章",
                        "本章没有确认的 beat，AI 可能自由发挥",
                        "先确认章纲再重新生成"));
            }
        }
        return issues;
    }

    private List<ChangeDto> proposeChanges(Chapter c) {
        List<ChangeDto> changes = new ArrayList<>();
        List<SettingEntity> entities = entityRepository.findByNovelId(c.getNovelId());
        String text = c.getContentMd();

        // 出场角色 → 角色状态更新提案
        for (SettingEntity e : entities) {
            if (e.getRevealChapter() != null && e.getRevealChapter().equals(c.getChapterNo())) {
                changes.add(new ChangeDto(
                        "c-ent-" + e.getId(),
                        "CHARACTER_UPDATE",
                        e.getName(),
                        "本章首次正式登场，登记当前状态",
                        null, null, null, true));
            }
        }
        // 时间线提案
        changes.add(new ChangeDto(
                "c-time-" + c.getId(),
                "TIMELINE",
                "时间线",
                "第 " + c.getChapterNo() + " 章主要事件",
                "待填写", null, null, true));
        // 伏笔提案：正文中出现悬念句式
        if (text.contains("……") || text.contains("没说") || text.contains("不知道")) {
            changes.add(new ChangeDto(
                    "c-plant-" + c.getId(),
                    "FORESHADOWING",
                    "伏笔",
                    "本章埋下的未解信息",
                    null, "埋", null, false));
        }
        return changes;
    }

    private void updateCharacter(Long novelId, ChangeDto ch) {
        entityRepository.findByNovelId(novelId).stream()
                .filter(e -> e.getName().equals(ch.name()))
                .findFirst()
                .ifPresent(e -> {
                    e.getCard().put("最新变化", ch.change());
                    e.setApprovalStatus("CONFIRMED");
                    entityRepository.save(e);
                });
    }

    private void createEntity(Long novelId, Integer chapterNo, ChangeDto ch) {
        boolean exists = entityRepository.findByNovelId(novelId).stream()
                .anyMatch(e -> e.getName().equals(ch.name()));
        if (exists) {
            return;
        }
        SettingEntity e = new SettingEntity();
        e.setNovelId(novelId);
        e.setEntityType(ch.entityType() == null ? "CHARACTER" : ch.entityType());
        e.setName(ch.name());
        e.setAliases(new ArrayList<>());
        e.getCard().put("设定", ch.change());
        e.setSource("AI");
        e.setApprovalStatus("CONFIRMED");
        e.setRevealChapter(chapterNo);
        entityRepository.save(e);
    }

    /** 摘要草稿：首版取正文开头 + 结尾，真实实现应交给模型（文档 §4 D 模板） */
    private String buildSummary(Chapter c) {
        String text = c.getContentMd();
        String head = text.length() > 120 ? text.substring(0, 120) : text;
        String tail = text.length() > 220 ? text.substring(text.length() - 100) : "";
        return "（摘要草稿，需人工修订）本章开头：" + head.replaceAll("\\s+", " ")
                + (tail.isEmpty() ? "" : "；结尾：" + tail.replaceAll("\\s+", " "));
    }

    private List<String> guessNames(String text) {
        List<String> out = new ArrayList<>();
        String[] pool = {"林昭", "灰羽", "苏晚", "裴临", "老陈", "岩甲蟹"};
        for (String n : pool) {
            if (text.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Chapter require(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("章节不存在"));
    }
}
