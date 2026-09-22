package com.quanlydetai.repository;

import com.quanlydetai.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository quản lý danh mục vai trò phân quyền hệ thống (RBAC: ROLE_SINHVIEN, ROLE_GIANGVIEN,...).
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Tìm đối tượng vai trò theo tên vai trò chuẩn (ví dụ: 'ROLE_SINHVIEN', 'ROLE_GIANGVIEN', 'ROLE_ADMIN').
     */
    Optional<Role> findByRoleName(String roleName);
}
