package com.quanlydetai.repository;

import com.quanlydetai.entity.SystemAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý dữ liệu nhật ký kiểm toán hệ thống (Audit Logs - theo dõi ai làm gì, khi nào).
 */
@Repository
public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {

    /**
     * Lấy 50 thao tác hệ thống mới nhất để hiển thị lên bảng điều khiển giám sát của Quản trị viên.
     */
    List<SystemAuditLog> findTop50ByOrderByCreatedAtDesc();
}
