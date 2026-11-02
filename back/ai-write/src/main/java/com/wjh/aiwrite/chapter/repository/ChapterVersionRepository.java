package com.wjh.aiwrite.chapter.repository;

import com.wjh.aiwrite.chapter.model.ChapterVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterVersionRepository extends JpaRepository<ChapterVersion, Long> {

    List<ChapterVersion> findByChapterIdOrderByVersionNoDesc(Long chapterId);
}
