package com.wjh.aiwrite.outline.dto;

import com.wjh.aiwrite.outline.model.Volume;

import java.util.ArrayList;
import java.util.List;

/** 卷 DTO */
public record VolumeDto(
        Long id,
        Long novelId,
        Integer volumeNo,
        String title,
        List<EventDto> events,
        String status,
        Integer targetChapters,
        String createdAt
) {
    public static VolumeDto of(Volume v) {
        List<EventDto> events = new ArrayList<>();
        List<String> raw = v.getEvents() == null ? List.of() : v.getEvents();
        for (int i = 0; i < raw.size(); i++) {
            events.add(new EventDto(i + 1, i + 1, raw.get(i)));
        }
        return new VolumeDto(
                v.getId(),
                v.getNovel().getId(),
                v.getVolumeNo(),
                v.getTitle(),
                events,
                v.getStatus(),
                v.getTargetChapters(),
                String.valueOf(v.getCreatedAt())
        );
    }
}
