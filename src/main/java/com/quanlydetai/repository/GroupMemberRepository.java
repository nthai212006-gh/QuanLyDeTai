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
}
