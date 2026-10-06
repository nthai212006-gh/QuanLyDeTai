package com.quanlydetai.repository;

import com.quanlydetai.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý danh sách sinh viên tham gia trong từng nhóm đề tài.
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /**
     * Lấy danh sách toàn bộ thành viên (gồm Trưởng nhóm và các Thành viên) của một nhóm theo ID nhóm.
     */
    List<GroupMember> findByGroupId(Long groupId);

    /**
     * Tìm thông tin thành viên của một sinh viên cụ thể trong một đợt đăng ký cụ thể.
     */
    Optional<GroupMember> findByStudentIdAndPeriodId(Long studentId, Long periodId);

    /**
     * Kiểm tra nhanh xem sinh viên này đã tham gia vào bất kỳ nhóm nào trong đợt này chưa (ràng buộc 1 SV chỉ 1 nhóm/đợt).
     */
    boolean existsByStudentIdAndPeriodId(Long studentId, Long periodId);

    /**
     * Đếm tổng số lượng thành viên hiện tại của một nhóm (dùng để chặn quy tắc tối đa 3 sinh viên / nhóm).
     */
    long countByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndStudentId(Long groupId, Long studentId);

    Optional<GroupMember> findFirstByGroupIdOrderByJoinedAtAsc(Long groupId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT gm.period.id FROM GroupMember gm WHERE gm.student.id = :studentId")
    java.util.Set<Long> findJoinedPeriodIdsByStudentId(@org.springframework.data.repository.query.Param("studentId") Long studentId);

    @org.springframework.data.jpa.repository.Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.period WHERE gm.student.id = :studentId ORDER BY gm.id DESC")
    List<GroupMember> findMembersWithPeriodByStudentId(@org.springframework.data.repository.query.Param("studentId") Long studentId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(gm) > 0 FROM GroupMember gm WHERE gm.student.id = :studentId AND gm.group.status NOT IN (:excludedStatuses)")
    boolean hasActiveGroupMembership(@org.springframework.data.repository.query.Param("studentId") Long studentId,
                                    @org.springframework.data.repository.query.Param("excludedStatuses") List<com.quanlydetai.entity.StudentGroup.GroupStatus> excludedStatuses);
}
