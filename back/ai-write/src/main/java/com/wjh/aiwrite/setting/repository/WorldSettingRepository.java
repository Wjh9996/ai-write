package com.wjh.aiwrite.setting.repository;

import com.wjh.aiwrite.setting.model.WorldSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorldSettingRepository extends JpaRepository<WorldSetting, Long> {

    List<WorldSetting> findByNovelId(Long novelId);
}
