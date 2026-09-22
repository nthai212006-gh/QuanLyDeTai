package com.quanlydetai.repository;

import com.quanlydetai.entity.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý các đợt đăng ký đề tài / khóa luận tốt nghiệp theo năm học và học kỳ.
 */
@Repository
public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

    /**
     * Lấy toàn bộ danh sách các đợt đăng ký, sắp xếp đợt mới tạo nhất lên đầu.
     */
    List<RegistrationPeriod> findAllByOrderByCreatedAtDesc();

    /**
     * Lấy danh sách các đợt đăng ký theo trạng thái hoạt động (isActive = true/false).
     */
    List<RegistrationPeriod> findByIsActive(Boolean isActive);
}
