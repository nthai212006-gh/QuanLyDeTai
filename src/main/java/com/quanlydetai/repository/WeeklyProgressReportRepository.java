package com.quanlydetai.repository;

import com.quanlydetai.entity.WeeklyProgressReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý báo cáo tiến độ tuần của các nhóm sinh viên làm đề tài.
 */
@Repository
public interface WeeklyProgressReportRepository extends JpaRepository<WeeklyProgressReport, Long> {

    /**
     * Lấy danh sách toàn bộ báo cáo tiến độ các tuần của một nhóm sinh viên, sắp xếp theo tuần tăng dần (Tuần 1, 2, 3...).
     */
    List<WeeklyProgressReport> findByGroupIdOrderByWeekNumberAsc(Long groupId);

    /**
     * Tìm báo cáo của một tuần cụ thể của nhóm sinh viên (ví dụ tìm báo cáo Tuần 5 của nhóm ID = 3).
     */
    Optional<WeeklyProgressReport> findByGroupIdAndWeekNumber(Long groupId, Integer weekNumber);
}
