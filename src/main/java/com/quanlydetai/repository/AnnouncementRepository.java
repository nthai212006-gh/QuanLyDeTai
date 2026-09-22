package com.quanlydetai.repository;

import com.quanlydetai.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý thông báo chung của hệ thống / khoa CNTT.
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    /**
     * Lấy danh sách thông báo hiển thị cho bảng tin:
     * Ưu tiên các bài được ghim (isPinned = true) lên trước, sau đó sắp xếp theo ngày tạo mới nhất giảm dần.
     */
    List<Announcement> findAllByOrderByIsPinnedDescCreatedAtDesc();
}
