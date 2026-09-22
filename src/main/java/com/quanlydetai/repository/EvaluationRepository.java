package com.quanlydetai.repository;

import com.quanlydetai.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý bảng điểm đánh giá chi tiết (Điểm GVHD, GVPB và từng thành viên Hội đồng).
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    /**
     * Lấy toàn bộ các phiếu điểm đánh giá thành phần của một phiên bảo vệ đề tài tại hội đồng.
     */
    List<Evaluation> findByCouncilTopicId(Long councilTopicId);

    /**
     * Tìm phiếu điểm cụ thể của một giảng viên chấm cho một phiên bảo vệ đề tài nhất định.
     */
    Optional<Evaluation> findByCouncilTopicIdAndEvaluatorLecturerId(Long councilTopicId, Long evaluatorLecturerId);
}
