package com.quanlydetai.repository;

import com.quanlydetai.entity.Council;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thông tin các hội đồng chấm bảo vệ khóa luận / đề tài.
 */
@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {

    /**
     * Tìm hội đồng theo mã hội đồng (duy nhất, ví dụ: HD01, HD_KHOALUAN_2026).
     */
    Optional<Council> findByCouncilCode(String councilCode);

    /**
     * Lấy danh sách tất cả các hội đồng được thành lập trong một đợt đăng ký cụ thể.
     */
    List<Council> findByPeriodId(Long periodId);

    /**
     * Lấy danh sách tất cả các hội đồng sắp xếp mới nhất trước.
     */
    List<Council> findAllByOrderByCreatedAtDesc();

    /**
     * Truy vấn trực tiếp từ View v_council_grading_summary theo mã ID hội đồng.
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT " +
            "council_id AS councilId, " +
            "council_name AS councilName, " +
            "council_code AS councilCode, " +
            "defense_order AS defenseOrder, " +
            "group_name AS groupName, " +
            "topic_title AS topicTitle, " +
            "reviewer_name AS reviewerName, " +
            "final_council_score AS finalCouncilScore, " +
            "is_published AS isPublished, " +
            "total_graded_members AS totalGradedMembers " +
            "FROM v_council_grading_summary WHERE council_id = :councilId", nativeQuery = true)
    List<com.quanlydetai.dto.CouncilGradingSummaryView> findGradingSummaryByCouncilId(@org.springframework.data.repository.query.Param("councilId") Long councilId);

    /**
     * Truy vấn toàn bộ bảng tổng hợp chấm điểm hội đồng từ View v_council_grading_summary.
     */
    @org.springframework.data.jpa.repository.Query(value = "SELECT " +
            "council_id AS councilId, " +
            "council_name AS councilName, " +
            "council_code AS councilCode, " +
            "defense_order AS defenseOrder, " +
            "group_name AS groupName, " +
            "topic_title AS topicTitle, " +
            "reviewer_name AS reviewerName, " +
            "final_council_score AS finalCouncilScore, " +
            "is_published AS isPublished, " +
            "total_graded_members AS totalGradedMembers " +
            "FROM v_council_grading_summary", nativeQuery = true)
    List<com.quanlydetai.dto.CouncilGradingSummaryView> findAllGradingSummaries();
}
