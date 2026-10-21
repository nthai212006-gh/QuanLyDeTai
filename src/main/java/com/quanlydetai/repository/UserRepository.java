package com.quanlydetai.repository;

import com.quanlydetai.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý người dùng (Sinh viên, Giảng viên, Giáo vụ, Trưởng khoa, Quản trị viên).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * Tìm tài khoản người dùng theo mã đăng nhập (Mã SV hoặc Mã Giảng viên).
     */
    Optional<User> findByUserCode(String userCode);

    /**
     * Tìm tài khoản người dùng theo địa chỉ Email cá nhân / trường học.
     */
    Optional<User> findByEmail(String email);

    /**
     * Tìm người dùng phục vụ xác thực đăng nhập Spring Security (chủ động FETCH roles để tránh LazyInitializationException).
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles " +
           "WHERE LOWER(u.userCode) = LOWER(:username) OR LOWER(u.email) = LOWER(:username)")
    Optional<User> findByUserCodeOrEmailWithRoles(@Param("username") String username);

    /**
     * Bước 2 của Two-phase Pagination: Nạp chi tiết quan hệ roles cho đúng tập ID của trang hiện tại (tránh N+1 và HHH000104).
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles " +
           "WHERE u.id IN (:ids)")
    List<User> findAllByIdsWithRolesAndDepartment(@Param("ids") Collection<Long> ids);

    /**
     * Lấy danh sách Giảng viên đang hoạt động (kèm roles để dropdown và form không bị N+1).
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE r.roleName = 'ROLE_LECTURER' AND u.isActive = true " +
           "  AND (:excludeUserId IS NULL OR u.id <> :excludeUserId) " +
           "ORDER BY u.fullName ASC")
    List<User> findActiveLecturers(@Param("excludeUserId") Long excludeUserId);

    /**
     * Kiểm tra xem mã người dùng đã tồn tại trên hệ thống chưa.
     */
    boolean existsByUserCode(String userCode);

    /**
     * Kiểm tra xem email đã được tài khoản khác sử dụng chưa.
     */
    boolean existsByEmail(String email);

    /**
     * Lấy danh sách tất cả người dùng trực thuộc một Khoa / Bộ môn cụ thể.
     */
    List<User> findByDepartmentId(Long departmentId);
}
