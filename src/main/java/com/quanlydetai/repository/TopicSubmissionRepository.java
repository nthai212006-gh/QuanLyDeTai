package com.quanlydetai.repository;

import com.quanlydetai.entity.TopicSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý các bài nộp tài liệu khóa luận (Đề cương, Báo cáo tiến độ, Báo cáo toàn văn, Slide, Link Source Code).
 */
@Repository
public interface TopicSubmissionRepository extends JpaRepository<TopicSubmission, Long> {

    /**
     * Lấy toàn bộ lịch sử các lần nộp tài liệu của một nhóm sinh viên, sắp xếp thời gian nộp mới nhất lên đầu.
     */
    List<TopicSubmission> findByGroupIdOrderBySubmittedAtDesc(Long groupId);
}
