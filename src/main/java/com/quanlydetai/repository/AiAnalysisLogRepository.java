package com.quanlydetai.repository;

import com.quanlydetai.entity.AiAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý dữ liệu lịch sử gọi AI phân tích (Kiểm tra trùng lặp, tóm tắt, gợi ý).
 */
@Repository
public interface AiAnalysisLogRepository extends JpaRepository<AiAnalysisLog, Long> {

    /**
     * Lấy toàn bộ lịch sử phân tích AI của một người dùng cụ thể, sắp xếp mới nhất lên đầu.
     */
    List<AiAnalysisLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Lấy danh sách nhật ký phân tích AI theo đợt đăng ký.
     */
    List<AiAnalysisLog> findByPeriodId(Long periodId);
}
