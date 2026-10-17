package com.quanlydetai.repository;

import com.quanlydetai.entity.AiAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository quản lý dữ liệu lịch sử gọi AI phân tích (Kiểm tra trùng lặp, tóm tắt, gợi ý, chat).
 */
@Repository
public interface AiAnalysisLogRepository extends JpaRepository<AiAnalysisLog, Long> {

    /**
     * Lấy toàn bộ lịch sử phân tích AI của một người dùng cụ thể, sắp xếp mới nhất lên đầu.
     */
    List<AiAnalysisLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Lấy lịch sử hội thoại gần nhất của người dùng.
     */
    List<AiAnalysisLog> findTop30ByUserIdAndActionTypeOrderByCreatedAtAsc(Long userId, String actionType);

    default List<AiAnalysisLog> findTop30ByUserIdAndAnalysisTypeOrderByCreatedAtAsc(Long userId, String analysisType) {
        return findTop30ByUserIdAndActionTypeOrderByCreatedAtAsc(userId, analysisType);
    }

    /**
     * Xóa toàn bộ lịch sử tin nhắn của người dùng.
     */
    @Modifying
    @Transactional
    void deleteByUserIdAndActionType(Long userId, String actionType);

    default void deleteByUserIdAndAnalysisType(Long userId, String analysisType) {
        deleteByUserIdAndActionType(userId, analysisType);
    }
}
