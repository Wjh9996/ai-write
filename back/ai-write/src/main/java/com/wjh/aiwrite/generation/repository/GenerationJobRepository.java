package com.wjh.aiwrite.generation.repository;

import com.wjh.aiwrite.generation.model.GenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, String> {

    Optional<GenerationJob> findByIdempotencyKey(String idempotencyKey);

    /** 规则⑦：同一章的活动任务（QUEUED/RUNNING/STOP_REQUESTED）最多一个 */
    @Query("""
            select j from GenerationJob j
            where j.chapterId = :chapterId
              and j.status in ('QUEUED','RUNNING','STOP_REQUESTED')
            """)
    List<GenerationJob> findActiveByChapter(@Param("chapterId") Long chapterId);
}
