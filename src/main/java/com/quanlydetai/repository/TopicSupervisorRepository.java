package com.quanlydetai.repository;

import com.quanlydetai.entity.TopicSupervisor;
import com.quanlydetai.entity.TopicSupervisorId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý việc phân công Giảng viên hướng dẫn cho đề tài (Hỗ trợ 1 đến 2 GV: Hướng dẫn chính và đồng hướng dẫn).
 */
@Repository
public interface TopicSupervisorRepository extends JpaRepository<TopicSupervisor, TopicSupervisorId> {

    /**
     * Lấy danh sách giảng viên hướng dẫn của một đề tài cụ thể.
     */
    List<TopicSupervisor> findByTopicId(Long topicId);

    /**
     * Lấy danh sách tất cả các đề tài mà một giảng viên cụ thể đang tham gia hướng dẫn.
     */
    List<TopicSupervisor> findByLecturerId(Long lecturerId);

    /**
     * Kiểm tra nhanh xem giảng viên này có đang hướng dẫn đề tài này hay không:
     * Dùng để kiểm tra quy tắc bất biến (Invariant): Giảng viên hướng dẫn không được phép tham gia chấm phản biện đề tài mình hướng dẫn.
     */
    boolean existsByTopicIdAndLecturerId(Long topicId, Long lecturerId);
}
