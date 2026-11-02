package com.wjh.aiwrite.review.repository;

import com.wjh.aiwrite.review.model.AuditReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditReportRepository extends JpaRepository<AuditReport, Long> {

    List<AuditReport> findByChapterIdOrderByIdDesc(Long chapterId);
}
