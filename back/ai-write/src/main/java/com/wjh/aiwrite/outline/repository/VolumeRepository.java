package com.wjh.aiwrite.outline.repository;

import com.wjh.aiwrite.outline.model.Volume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VolumeRepository extends JpaRepository<Volume, Long> {

    List<Volume> findByNovelIdOrderByVolumeNo(Long novelId);

    int countByNovelId(Long novelId);
}
