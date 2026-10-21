package com.quanlydetai.repository.specification;

import com.quanlydetai.entity.Role;
import com.quanlydetai.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification động cho tìm kiếm User đa tiêu chí.
 * Loại bỏ anti-pattern (:param IS NULL OR col = :param) để database tận dụng tối đa B-Tree index scan.
 * TUYỆT ĐỐI KHÔNG FETCH quan hệ collection ở đây để phục vụ Two-phase ID-Pagination chuẩn.
 */
public class UserSpecifications {

    private UserSpecifications() {
        // utility class
    }

    public static Specification<User> filterUsers(String keyword, Long roleId, Long departmentId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo từ khóa keyword (Mã người dùng, Họ tên, Email)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate codeMatch = cb.like(cb.lower(root.get("userCode")), pattern);
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch, emailMatch));
            }

            // 2. Lọc theo vai trò roleId (chỉ INNER JOIN để lọc, KHÔNG FETCH collection)
            if (roleId != null) {
                Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
                predicates.add(cb.equal(roleJoin.get("id"), roleId));
            }

            // 3. Lọc theo bộ môn departmentId
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            // Loại bỏ dòng trùng lặp khi join collection
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
