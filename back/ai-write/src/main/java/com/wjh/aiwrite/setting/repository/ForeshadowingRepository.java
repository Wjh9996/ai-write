package com.wjh.aiwrite.setting.repository;

import com.wjh.aiwrite.setting.model.Foreshadowing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForeshadowingRepository extends JpaRepository<Foreshadowing, Long> {

    List<Foreshadowing> findByNovelId(Long novelId);
}
