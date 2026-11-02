package com.wjh.aiwrite.setting.repository;

import com.wjh.aiwrite.setting.model.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, Long> {

    List<TimelineEvent> findByNovelIdOrderBySortNo(Long novelId);
}
