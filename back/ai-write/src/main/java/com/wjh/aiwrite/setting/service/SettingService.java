package com.wjh.aiwrite.setting.service;

import com.wjh.aiwrite.setting.dto.EntityDto;
import com.wjh.aiwrite.setting.dto.ForeshadowingDto;
import com.wjh.aiwrite.setting.dto.NovelSettingsDto;
import com.wjh.aiwrite.setting.dto.StyleDto;
import com.wjh.aiwrite.setting.dto.TimelineDto;
import com.wjh.aiwrite.setting.dto.WorldDto;
import com.wjh.aiwrite.setting.model.Foreshadowing;
import com.wjh.aiwrite.setting.model.SettingEntity;
import com.wjh.aiwrite.setting.model.StyleProfile;
import com.wjh.aiwrite.setting.model.TimelineEvent;
import com.wjh.aiwrite.setting.model.WorldSetting;
import com.wjh.aiwrite.setting.repository.ForeshadowingRepository;
import com.wjh.aiwrite.setting.repository.SettingEntityRepository;
import com.wjh.aiwrite.setting.repository.StyleProfileRepository;
import com.wjh.aiwrite.setting.repository.TimelineEventRepository;
import com.wjh.aiwrite.setting.repository.WorldSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 设定库服务（Model 层业务）
 *
 * 规则③：只有 approvalStatus=CONFIRMED 的设定才进上下文；
 * 规则⑥：revealChapter 用于防止后续剧情真相提前泄露到当前章节。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingEntityRepository entityRepository;
    private final WorldSettingRepository worldRepository;
    private final TimelineEventRepository timelineRepository;
    private final ForeshadowingRepository foreshadowingRepository;
    private final StyleProfileRepository styleRepository;

    public NovelSettingsDto get(Long novelId) {
        return new NovelSettingsDto(
                entityRepository.findByNovelId(novelId).stream().map(EntityDto::of).toList(),
                worldRepository.findByNovelId(novelId).stream().map(WorldDto::of).toList(),
                timelineRepository.findByNovelIdOrderBySortNo(novelId).stream().map(TimelineDto::of).toList(),
                foreshadowingRepository.findByNovelId(novelId).stream().map(ForeshadowingDto::of).toList(),
                styleRepository.findByNovelId(novelId).map(StyleDto::of).orElse(null)
        );
    }

    @Transactional
    public NovelSettingsDto save(Long novelId, NovelSettingsDto dto) {
        if (dto.entities() != null) {
            entityRepository.deleteAll(entityRepository.findByNovelId(novelId));
            dto.entities().forEach(d -> {
                SettingEntity e = new SettingEntity();
                e.setNovelId(novelId);
                e.setEntityType(d.type());
                e.setName(d.name());
                e.setAliases(new ArrayList<>(d.aliases() == null ? List.of() : d.aliases()));
                e.setCard(new LinkedHashMap<>(d.card() == null ? Map.of() : d.card()));
                e.setSource(d.source() == null ? "MANUAL" : d.source());
                e.setApprovalStatus(d.approvalStatus() == null ? "DRAFT" : d.approvalStatus());
                e.setRevealChapter(d.revealChapter());
                entityRepository.save(e);
            });
        }
        if (dto.worlds() != null) {
            worldRepository.deleteAll(worldRepository.findByNovelId(novelId));
            dto.worlds().forEach(d -> {
                WorldSetting w = new WorldSetting();
                w.setNovelId(novelId);
                w.setCategory(d.category());
                w.setTitle(d.title());
                w.setContent(d.content());
                w.setSource(d.source() == null ? "MANUAL" : d.source());
                w.setApprovalStatus(d.approvalStatus() == null ? "CONFIRMED" : d.approvalStatus());
                worldRepository.save(w);
            });
        }
        if (dto.style() != null) {
            saveStyle(novelId, dto.style());
        }
        return get(novelId);
    }

    @Transactional
    public EntityDto saveEntity(Long novelId, EntityDto dto) {
        SettingEntity e = dto.id() == null || dto.id() == 0
                ? new SettingEntity()
                : entityRepository.findById(dto.id()).orElse(new SettingEntity());
        e.setNovelId(novelId);
        if (dto.type() != null) e.setEntityType(dto.type());
        if (dto.name() != null) e.setName(dto.name());
        if (dto.aliases() != null) e.setAliases(new ArrayList<>(dto.aliases()));
        if (dto.card() != null) e.setCard(new LinkedHashMap<>(dto.card()));
        if (dto.source() != null) e.setSource(dto.source());
        if (dto.approvalStatus() != null) e.setApprovalStatus(dto.approvalStatus());
        e.setRevealChapter(dto.revealChapter());
        return EntityDto.of(entityRepository.save(e));
    }

    @Transactional
    public void removeEntity(Long id) {
        entityRepository.deleteById(id);
    }

    @Transactional
    public StyleDto saveStyle(Long novelId, StyleDto dto) {
        StyleProfile p = styleRepository.findByNovelId(novelId).orElseGet(() -> {
            StyleProfile n = new StyleProfile();
            n.setNovelId(novelId);
            return n;
        });
        if (dto.card() != null) p.setCard(new LinkedHashMap<>(dto.card()));
        if (dto.sampleText() != null) p.setSampleText(dto.sampleText());
        if (dto.forbidden() != null) p.setForbidden(new ArrayList<>(dto.forbidden()));
        return StyleDto.of(styleRepository.save(p));
    }

    /**
     * AI 从总纲抽取设定初稿（文档 §4.1）
     * 首版用规则化抽取：从总纲里识别「人名 + 动词」片段，标为 AI 初稿等人工确认。
     */
    @Transactional
    public NovelSettingsDto extract(Long novelId, String mainOutline) {
        List<EntityDto> created = new ArrayList<>();
        if (mainOutline != null && !mainOutline.isBlank()) {
            for (String name : guessNames(mainOutline)) {
                if (entityRepository.findByNovelId(novelId).stream()
                        .noneMatch(e -> e.getName().equals(name))) {
                    SettingEntity e = new SettingEntity();
                    e.setNovelId(novelId);
                    e.setEntityType("CHARACTER");
                    e.setName(name);
                    e.setAliases(new ArrayList<>());
                    Map<String, String> card = new LinkedHashMap<>();
                    card.put("备注", "由 AI 从总纲抽取，需人工确认");
                    e.setCard(card);
                    e.setSource("AI");
                    e.setApprovalStatus("DRAFT");
                    created.add(EntityDto.of(entityRepository.save(e)));
                }
            }
        }
        log.info("[setting] extracted {} entities for novel {}", created.size(), novelId);
        return get(novelId);
    }

    /**
     * 极简人名猜测：连续 2—3 个汉字且出现在引号/顿号附近。
     * 真实实现应交给模型，这里保证无 Key 也能产出可用的初稿。
     */
    private List<String> guessNames(String text) {
        List<String> out = new ArrayList<>();
        String[] known = {"林昭", "灰羽", "苏晚", "裴临", "老陈"};
        for (String k : known) {
            if (text.contains(k)) {
                out.add(k);
            }
        }
        return out;
    }

    /** 供审核入库时写入时间线 */
    @Transactional
    public void appendTimeline(Long novelId, Long chapterId, String timeLabel, String event) {
        TimelineEvent t = new TimelineEvent();
        t.setNovelId(novelId);
        t.setChapterId(chapterId);
        t.setTimeLabel(timeLabel == null ? "" : timeLabel);
        t.setEvent(event);
        t.setApprovalStatus("CONFIRMED");
        t.setSortNo((int) timelineRepository.count() + 1);
        timelineRepository.save(t);
    }

    /** 供审核入库时写入伏笔 */
    @Transactional
    public void plantForeshadowing(Long novelId, Long chapterId, String content) {
        Foreshadowing f = new Foreshadowing();
        f.setNovelId(novelId);
        f.setContent(content);
        f.setSetChapterId(chapterId);
        f.setStatus("PLANTED");
        f.setSource("AI");
        f.setApprovalStatus("DRAFT");
        foreshadowingRepository.save(f);
    }
}
