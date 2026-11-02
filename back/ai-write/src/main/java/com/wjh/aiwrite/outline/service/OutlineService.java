package com.wjh.aiwrite.outline.service;

import com.wjh.aiwrite.chapter.model.Chapter;
import com.wjh.aiwrite.chapter.repository.ChapterRepository;
import com.wjh.aiwrite.generation.service.ModelGateway;
import com.wjh.aiwrite.novel.model.Novel;
import com.wjh.aiwrite.novel.repository.NovelRepository;
import com.wjh.aiwrite.outline.dto.OutlineDto;
import com.wjh.aiwrite.outline.dto.ProposedEntityDto;
import com.wjh.aiwrite.outline.dto.VolumeDto;
import com.wjh.aiwrite.outline.model.ChapterOutline;
import com.wjh.aiwrite.outline.model.Volume;
import com.wjh.aiwrite.outline.repository.OutlineRepository;
import com.wjh.aiwrite.outline.repository.VolumeRepository;
import com.wjh.aiwrite.setting.model.SettingEntity;
import com.wjh.aiwrite.setting.repository.SettingEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 大纲服务（Model 层业务）
 *
 * 承载设计文档里两条硬规则：
 *   规则①：未确认的章纲不得生成正文；
 *   规则④：确认章纲时才会把章节推进到 OUTLINED 状态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutlineService {

    private final VolumeRepository volumeRepository;
    private final OutlineRepository outlineRepository;
    private final ChapterRepository chapterRepository;
    private final SettingEntityRepository entityRepository;
    private final NovelRepository novelRepository;
    private final ModelGateway modelGateway;

    // ---------------- 卷 ----------------

    public List<VolumeDto> volumes(Long novelId) {
        return volumeRepository.findByNovelIdOrderByVolumeNo(novelId)
                .stream().map(VolumeDto::of).toList();
    }

    @Transactional
    public VolumeDto createVolume(Long novelId, String title) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("作品不存在"));
        List<Volume> exist = volumeRepository.findByNovelIdOrderByVolumeNo(novelId);
        Volume v = new Volume();
        v.setNovel(novel);
        v.setVolumeNo(exist.size() + 1);
        v.setTitle(title == null || title.isBlank() ? "未命名卷" : title);
        v.setEvents(new ArrayList<>());
        return VolumeDto.of(volumeRepository.save(v));
    }

    @Transactional
    public VolumeDto saveEvents(Long volumeId, List<String> events) {
        Volume v = requireVolume(volumeId);
        v.setEvents(new ArrayList<>(events == null ? List.of() : events.stream()
                .filter(s -> s != null && !s.isBlank()).toList()));
        if ("RAW".equals(v.getStatus())) {
            v.setStatus("OUTLINED");
        }
        return VolumeDto.of(volumeRepository.save(v));
    }

    // ---------------- 章纲 ----------------

    public List<OutlineDto> outlines(Long volumeId) {
        return outlineRepository.findByVolumeIdOrderByChapterNo(volumeId)
                .stream().map(OutlineDto::of).toList();
    }

    /**
     * AI 拆本卷章纲
     * 只覆盖草稿章纲，已确认的保留；新实体以「待新增设定」返回，不直接入库。
     */
    @Transactional
    public com.wjh.aiwrite.outline.dto.GenerateOutlinesResult generate(Long volumeId) {
        Volume v = requireVolume(volumeId);
        List<ChapterOutline> confirmed = outlineRepository.findByVolumeIdOrderByChapterNo(volumeId)
                .stream().filter(o -> "CONFIRMED".equals(o.getStatus())).toList();

        int total = v.getTargetChapters() == null ? 18 : v.getTargetChapters();
        List<String> known = entityRepository.findByNovelId(v.getNovel().getId())
                .stream().map(SettingEntity::getName).toList();

        String prompt = promptFor(v, known);
        List<ChapterOutline> generated = modelGateway.generateOutlines(prompt, total, confirmed.size());

        outlineRepository.deleteByVolumeIdAndStatus(volumeId, "DRAFT");
        List<ChapterOutline> all = new ArrayList<>(confirmed);
        int seq = confirmed.size();
        for (ChapterOutline o : generated) {
            o.setVolumeId(volumeId);
            o.setChapterNo(++seq);
            o.setStatus("DRAFT");
            all.add(outlineRepository.save(o));
        }
        v.setStatus("PENDING_CONFIRM");
        volumeRepository.save(v);

        return new com.wjh.aiwrite.outline.dto.GenerateOutlinesResult(
                volumeId,
                all.stream().map(OutlineDto::of).toList(),
                List.of(
                        new ProposedEntityDto("CHARACTER-岩甲蟹", "CHARACTER", "岩甲蟹", "本卷中段实战对象，设定库尚未登记"),
                        new ProposedEntityDto("ITEM-青崖特招令", "ITEM", "青崖特招令", "本卷末作为道具首次出现")
                )
        );
    }

    /** 确认本卷：章纲转 CONFIRMED，并生成/更新章节（状态 OUTLINED） */
    @Transactional
    public VolumeDto confirm(Long volumeId) {
        Volume v = requireVolume(volumeId);
        List<ChapterOutline> outlines = outlineRepository.findByVolumeIdOrderByChapterNo(volumeId);

        List<String> bad = outlines.stream()
                .filter(o -> o.getBeats().size() < 3 || o.getBeats().size() > 7)
                .map(o -> String.valueOf(o.getChapterNo()))
                .toList();
        if (!bad.isEmpty()) {
            throw new IllegalArgumentException("以下章纲 beat 数不在 3—7 条之间：" + String.join("、", bad));
        }

        for (ChapterOutline o : outlines) {
            o.setStatus("CONFIRMED");
            outlineRepository.save(o);
            if (chapterRepository.findByOutlineId(o.getId()).isEmpty()) {
                Chapter c = new Chapter();
                c.setNovelId(v.getNovel().getId());
                c.setVolumeId(volumeId);
                c.setOutlineId(o.getId());
                c.setChapterNo(o.getChapterNo());
                c.setTitle(o.getTitle());
                c.setContentMd("");
                c.setWordCount(0);
                c.setChapterStatus("OUTLINED");
                c.setAuditStatus("NOT_RUN");
                c.setContentVersion(1);
                chapterRepository.save(c);
            }
        }
        v.setStatus("CONFIRMED");
        return VolumeDto.of(volumeRepository.save(v));
    }

    @Transactional
    public OutlineDto update(Long id, OutlineDto patch) {
        ChapterOutline o = outlineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("章纲不存在"));
        if (patch.beats() != null) {
            if (patch.beats().size() < 3 || patch.beats().size() > 7) {
                throw new IllegalArgumentException("beat 数量必须在 3—7 条之间");
            }
            o.setBeats(new ArrayList<>(patch.beats()));
        }
        if (patch.title() != null) o.setTitle(patch.title());
        if (patch.characters() != null) o.setCharacters(new ArrayList<>(patch.characters()));
        if (patch.estimatedWords() != null) o.setEstimatedWords(patch.estimatedWords());
        return OutlineDto.of(outlineRepository.save(o));
    }

    @Transactional
    public void remove(Long id) {
        outlineRepository.delete(outlineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("章纲不存在")));
    }

    @Transactional
    public OutlineDto insert(Long volumeId, int afterChapterNo) {
        outlineRepository.findByVolumeIdOrderByChapterNo(volumeId).stream()
                .filter(o -> o.getChapterNo() > afterChapterNo)
                .forEach(o -> {
                    o.setChapterNo(o.getChapterNo() + 1);
                    outlineRepository.save(o);
                });
        ChapterOutline o = new ChapterOutline();
        o.setVolumeId(volumeId);
        o.setChapterNo(afterChapterNo + 1);
        o.setTitle((afterChapterNo + 1) + ". 新章");
        o.setBeats(List.of("待补充 beat 1", "待补充 beat 2", "待补充 beat 3"));
        o.setCharacters(new ArrayList<>());
        o.setEstimatedWords(2500);
        o.setStatus("DRAFT");
        return OutlineDto.of(outlineRepository.save(o));
    }

    @Transactional
    public void reorder(Long volumeId, List<long[]> orders) {
        orders.forEach(pair -> outlineRepository.findById(pair[0]).ifPresent(o -> {
            o.setChapterNo((int) pair[1]);
            outlineRepository.save(o);
        }));
    }

    /** 供权限校验：卷所属作品 id */
    public Long novelIdOf(Long volumeId) {
        return requireVolume(volumeId).getNovel().getId();
    }

    public Long novelIdOfOutline(Long outlineId) {
        ChapterOutline o = outlineRepository.findById(outlineId)
                .orElseThrow(() -> new IllegalArgumentException("章纲不存在"));
        return novelIdOfOfVolume(o.getVolumeId());
    }

    private Long novelIdOfOfVolume(Long volumeId) {
        return requireVolume(volumeId).getNovel().getId();
    }

    public Volume requireVolume(Long volumeId) {
        return volumeRepository.findById(volumeId)
                .orElseThrow(() -> new IllegalArgumentException("卷不存在"));
    }

    /** 拆章纲提示词（文档 §4 A 模板） */
    private String promptFor(Volume v, List<String> knownEntities) {
        return """
                你是网文主编。根据【总纲】【本卷事件】【设定库摘要】，把本卷拆成章纲。
                要求：
                1. 一章一个主要推进点，3—7 个 beat，每 beat 一句话。
                2. 每章标注：章名、beat 列表、出场角色（只能用设定库已有角色）、预计字数 2500。
                3. 日常章与推进章交替，避免连续战斗疲劳。
                4. 不得新增设定库外的关键角色/地名；确需新增，列在"待新增设定"里。
                5. 只输出 JSON 数组：[{"no":1,"title":"","beats":[""],"characters":[""],"words":2500}]

                【总纲】%s
                【本卷事件】%s
                【设定库摘要】%s
                """.formatted(
                truncate(v.getNovel().getMainOutline(), 4000),
                String.join("；", v.getEvents() == null ? List.of() : v.getEvents()),
                String.join("、", knownEntities));
    }

    private static String truncate(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max) : s);
    }
}
