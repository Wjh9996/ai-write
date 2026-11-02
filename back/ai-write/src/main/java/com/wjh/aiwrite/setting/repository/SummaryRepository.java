package com.wjh.aiwrite.setting.repository;

import com.wjh.aiwrite.setting.model.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, Long> {

    /** 取已确认的摘要（只有 CONFIRMED 才进上下文，规则③） */
    List<Summary> findByNovelIdAndScopeOrderByIdDesc(Long novelId, String scope);
}
