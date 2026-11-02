package com.wjh.aiwrite.outline.repository;

import com.wjh.aiwrite.outline.model.ChapterOutline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutlineRepository extends JpaRepository<ChapterOutline, Long> {

    List<ChapterOutline> findByVolumeIdOrderByChapterNo(Long volumeId);

    /** 重新生成时只清理草稿，已确认的章纲保留 */
    @Modifying
    @Query("delete from ChapterOutline o where o.volumeId = :volumeId and o.status = :status")
    int deleteByVolumeIdAndStatus(@Param("volumeId") Long volumeId, @Param("status") String status);
}
