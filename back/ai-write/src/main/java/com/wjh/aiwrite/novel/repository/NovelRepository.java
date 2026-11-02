package com.wjh.aiwrite.novel.repository;

import com.wjh.aiwrite.novel.model.Novel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {

    List<Novel> findByUserIdOrderByIdDesc(Long userId);
}
