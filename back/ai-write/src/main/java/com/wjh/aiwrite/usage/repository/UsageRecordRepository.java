package com.wjh.aiwrite.usage.repository;

import com.wjh.aiwrite.usage.model.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    List<UsageRecord> findTop100ByOrderByCreatedAtDesc();
}
