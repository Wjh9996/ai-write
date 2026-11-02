package com.wjh.aiwrite.setting.repository;

import com.wjh.aiwrite.setting.model.StyleProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StyleProfileRepository extends JpaRepository<StyleProfile, Long> {

    Optional<StyleProfile> findByNovelId(Long novelId);
}
