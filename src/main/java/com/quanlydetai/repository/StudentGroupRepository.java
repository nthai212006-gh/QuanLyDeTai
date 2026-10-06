package com.quanlydetai.repository;

import com.quanlydetai.entity.StudentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thông tin các nhóm sinh viên làm đề tài nghiên cứu.
 */
@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    /**
     * Tìm nhóm sinh viên theo tên nhóm.
     */
    Optional<StudentGroup> findByGroupName(String groupName);

    /**
     * Lấy danh sách toàn bộ các nhóm sinh viên đăng ký trong một đợt cụ thể.
     */
    List<StudentGroup> findByPeriodId(Long periodId);

    /**
     * Lấy tất cả các nhóm sinh viên sắp xếp mới nhất trước.
     */
    List<StudentGroup> findAllByOrderByCreatedAtDesc();

    /**
     * Tìm nhóm sinh viên do một sinh viên cụ thể làm nhóm trưởng trong đợt đăng ký đó.
     */
    Optional<StudentGroup> findByLeaderIdAndPeriodId(Long leaderId, Long periodId);

    /**
     * Truy vấn tìm nhóm sinh viên mà một sinh viên bất kỳ đang thuộc về trong đợt đó (kể cả nhóm trưởng hay thành viên).
     */
    @Query("SELECT gm.group FROM GroupMember gm WHERE gm.student.id = :studentId AND gm.period.id = :periodId")
    Optional<StudentGroup> findGroupByStudentAndPeriod(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    /**
     * Truy vấn tìm các nhóm sinh viên đủ điều kiện để phân công vào hội đồng bảo vệ:
     * Điều kiện: Thuộc đợt này, đã có đề tài được duyệt (APPROVED), và chưa được phân vào bất kỳ hội đồng nào.
     */
    @Query("SELECT g FROM StudentGroup g WHERE g.period.id = :periodId " +
           "AND g.topic IS NOT NULL AND g.topic.status = 'APPROVED' " +
           "AND g.id NOT IN (SELECT ct.group.id FROM CouncilTopic ct)")
    List<StudentGroup> findAvailableForCouncilAssignment(@Param("periodId") Long periodId);
}
