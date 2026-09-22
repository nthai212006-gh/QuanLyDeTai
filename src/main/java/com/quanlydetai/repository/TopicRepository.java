package com.quanlydetai.repository;

import com.quanlydetai.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu kho đề tài khóa luận / đồ án tốt nghiệp.
 */
@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {

    /**
     * Tìm đề tài theo mã đề tài duy nhất (ví dụ: 'DT001', 'KLTN_2026_01').
     */
    Optional<Topic> findByTopicCode(String topicCode);

    /**
     * Lấy danh sách toàn bộ đề tài thuộc về một đợt đăng ký cụ thể.
     */
    List<Topic> findByPeriodId(Long periodId);

    /**
     * Lấy danh sách các đề tài thuộc quản lý của một Khoa / Bộ môn cụ thể.
     */
    List<Topic> findByDepartmentId(Long departmentId);

    /**
     * Lấy danh sách đề tài theo đợt và trạng thái duyệt (ví dụ: các đề tài APPROVED trong đợt 1).
     */
    List<Topic> findByPeriodIdAndStatus(Long periodId, Topic.TopicStatus status);

    /**
     * Lấy danh sách tất cả các đề tài do một giảng viên cụ thể đề xuất / tạo ra.
     */
    List<Topic> findByCreatedByLecturerId(Long lecturerId);

    /**
     * Lấy danh sách các đề tài hợp lệ để sinh viên có thể đăng ký:
     * Điều kiện: Thuộc đợt đăng ký này, đã được Bộ môn phê duyệt (APPROVED), và chưa bị nhóm sinh viên nào đăng ký trước đó.
     */
    @Query("SELECT t FROM Topic t WHERE t.period.id = :periodId AND t.status = 'APPROVED' AND t.id NOT IN " +
           "(SELECT g.topic.id FROM StudentGroup g WHERE g.topic IS NOT NULL AND g.period.id = :periodId)")
    List<Topic> findAvailableTopicsForRegistration(@Param("periodId") Long periodId);

    /**
     * Truy vấn trực tiếp từ View v_topic_registration_status trong cơ sở dữ liệu.
     */
    @Query(value = "SELECT " +
            "topic_id AS topicId, " +
            "topic_code AS topicCode, " +
            "topic_title AS topicTitle, " +
            "department_name AS departmentName, " +
            "period_name AS periodName, " +
            "primary_supervisor AS primarySupervisor, " +
            "topic_status AS topicStatus, " +
            "assigned_group_id AS assignedGroupId, " +
            "assigned_group_name AS assignedGroupName " +
            "FROM v_topic_registration_status", nativeQuery = true)
    List<com.quanlydetai.dto.TopicRegistrationStatusView> findAllTopicRegistrationStatus();
}
