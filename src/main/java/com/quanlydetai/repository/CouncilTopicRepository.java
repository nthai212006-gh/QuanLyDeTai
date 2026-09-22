package com.quanlydetai.repository;

import com.quanlydetai.entity.CouncilTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý việc phân công đề tài / nhóm sinh viên vào hội đồng bảo vệ.
 */
@Repository
public interface CouncilTopicRepository extends JpaRepository<CouncilTopic, Long> {

    /**
     * Lấy danh sách tất cả các nhóm đề tài được xếp lịch bảo vệ trước một hội đồng cụ thể.
     */
    List<CouncilTopic> findByCouncilId(Long councilId);

    /**
     * Tìm thông tin lịch bảo vệ và hội đồng được gán cho một nhóm sinh viên cụ thể.
     */
    Optional<CouncilTopic> findByGroupId(Long groupId);

    /**
     * Tìm chi tiết lịch bảo vệ của một nhóm cụ thể trong một hội đồng cụ thể.
     */
    Optional<CouncilTopic> findByCouncilIdAndGroupId(Long councilId, Long groupId);
}
