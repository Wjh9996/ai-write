package com.wjh.aiwrite.chapter.repository;

import com.wjh.aiwrite.chapter.model.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findByNovelIdOrderByChapterNo(Long novelId);

    Optional<Chapter> findByOutlineId(Long outlineId);
}
