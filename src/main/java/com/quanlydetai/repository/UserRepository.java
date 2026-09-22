package com.quanlydetai.repository;

import com.quanlydetai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý người dùng (Sinh viên, Giảng viên, Giáo vụ, Trưởng khoa, Quản trị viên).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm tài khoản người dùng theo mã đăng nhập (Mã SV hoặc Mã Giảng viên).
     */
    Optional<User> findByUserCode(String userCode);

    /**
     * Tìm tài khoản người dùng theo địa chỉ Email cá nhân / trường học.
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiểm tra xem mã người dùng đã tồn tại trên hệ thống chưa (dùng khi kiểm tra trùng mã lúc thêm mới).
     */
    boolean existsByUserCode(String userCode);

    /**
     * Kiểm tra xem email đã được tài khoản khác sử dụng chưa (dùng khi đăng ký hoặc cập nhật hồ sơ).
     */
    boolean existsByEmail(String email);

    /**
     * Lấy danh sách tất cả người dùng (Giảng viên / Sinh viên) trực thuộc một Khoa / Bộ môn cụ thể.
     */
    List<User> findByDepartmentId(Long departmentId);
}
