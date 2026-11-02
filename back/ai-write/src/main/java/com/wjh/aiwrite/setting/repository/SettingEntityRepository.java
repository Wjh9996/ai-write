package com.wjh.aiwrite.setting.repository;

import com.wjh.aiwrite.setting.model.SettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettingEntityRepository extends JpaRepository<SettingEntity, Long> {

    List<SettingEntity> findByNovelId(Long novelId);
}
