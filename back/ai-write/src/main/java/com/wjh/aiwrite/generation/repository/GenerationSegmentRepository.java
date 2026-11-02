package com.wjh.aiwrite.generation.repository;

import com.wjh.aiwrite.generation.model.GenerationSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationSegmentRepository extends JpaRepository<GenerationSegment, Long> {

    List<GenerationSegment> findByJobIdOrderBySegmentNo(String jobId);

    Optional<GenerationSegment> findByJobIdAndSegmentNo(String jobId, Integer segmentNo);

    /** 重生成时清理旧草稿 */
    @Modifying
    @Query("delete from GenerationSegment s where s.jobId = :jobId and s.segmentNo >= :from")
    int deleteFrom(@Param("jobId") String jobId, @Param("from") Integer from);
}
