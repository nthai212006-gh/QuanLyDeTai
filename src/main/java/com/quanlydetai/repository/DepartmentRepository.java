package com.quanlydetai.repository;

import com.quanlydetai.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý thông tin các Khoa / Bộ môn chuyên ngành (ví dụ: CNTT, HTTT, KHMT).
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Tìm thông tin Khoa / Bộ môn theo mã định danh duy nhất (ví dụ: 'CNTT', 'KHMT').
     */
    Optional<Department> findByDeptCode(String deptCode);
}
