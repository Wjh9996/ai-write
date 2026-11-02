package com.wjh.aiwrite.review.repository;

import com.wjh.aiwrite.review.model.SettingChangeDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingChangeDraftRepository extends JpaRepository<SettingChangeDraft, Long> {

    List<SettingChangeDraft> findByChapterIdOrderByIdDesc(Long chapterId);
}
