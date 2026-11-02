package com.wjh.aiwrite.generation.repository;

import com.wjh.aiwrite.generation.model.GenerationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenerationEventRepository extends JpaRepository<GenerationEvent, Long> {

    /** 断线补读：取 seq 大于 lastEventId 的历史事件 */
    List<GenerationEvent> findByJobIdAndEventSeqGreaterThanOrderByEventSeq(String jobId, Long eventSeq);
}
