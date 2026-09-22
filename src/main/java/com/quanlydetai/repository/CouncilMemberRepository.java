package com.quanlydetai.repository;

import com.quanlydetai.entity.CouncilMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý thành viên và vai trò trong hội đồng bảo vệ (Chủ tịch, Thư ký, Ủy viên).
 */
@Repository
public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {

    /**
     * Lấy danh sách tất cả các giảng viên tham gia trong một hội đồng cụ thể.
     */
    List<CouncilMember> findByCouncilId(Long councilId);

    /**
     * Lấy danh sách tất cả các hội đồng mà một giảng viên đang là thành viên.
     */
    List<CouncilMember> findByLecturerId(Long lecturerId);

    /**
     * Tìm thông tin vai trò của một giảng viên cụ thể trong một hội đồng cụ thể.
     */
    Optional<CouncilMember> findByCouncilIdAndLecturerId(Long councilId, Long lecturerId);

    /**
     * Đếm tổng số lượng thành viên hiện tại của một hội đồng (dùng để kiểm tra quy tắc đủ 3 đến 5 người).
     */
    long countByCouncilId(Long councilId);

    /**
     * Kiểm tra một chức vụ (CHAIR/SECRETARY) đã có người đảm nhận trong hội đồng chưa.
     */
    boolean existsByCouncilIdAndPosition(Long councilId, CouncilMember.CouncilPosition position);

    /**
     * Kiểm tra một giảng viên cụ thể có đang giữ chức vụ CHAIR trong hội đồng không.
     */
    boolean existsByCouncilIdAndLecturerIdAndPosition(Long councilId, Long lecturerId,
                                                      CouncilMember.CouncilPosition position);
}
