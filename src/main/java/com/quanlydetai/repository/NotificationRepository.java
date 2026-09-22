package com.quanlydetai.repository;

import com.quanlydetai.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý thông báo cá nhân (Notification Bell và thông báo Email) gửi tới người dùng.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy tất cả thông báo của một người dùng, sắp xếp thông báo mới nhất lên đầu.
     */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    /**
     * Lấy danh sách các thông báo chưa đọc của người dùng, sắp xếp mới nhất lên đầu.
     */
    List<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId);



    /**
     * Đếm số lượng thông báo chưa đọc của một người dùng (dùng để hiển thị số badge đỏ trên chuông thông báo).
     */
    long countByRecipientIdAndIsReadFalse(Long recipientId);
}
