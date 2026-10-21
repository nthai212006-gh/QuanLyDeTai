package com.quanlydetai.service;

import com.quanlydetai.dto.UserCreateDto;
import com.quanlydetai.entity.Department;
import com.quanlydetai.entity.Role;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.DepartmentRepository;
import com.quanlydetai.repository.RoleRepository;
import com.quanlydetai.repository.UserRepository;
import com.quanlydetai.repository.specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessionRegistry;

    /**
     * Tìm kiếm và phân trang người dùng bằng mô hình Two-phase ID-Pagination chuẩn.
     * Bước 1: Query phân trang chuẩn JpaSpecificationExecutor (SQL LIMIT ? OFFSET ? trên DB, không fetch collection).
     * Bước 2: Nạp chi tiết đầy đủ roles cho đúng các ID của trang đó (Triệt tiêu N+1 và tránh HHH000104).
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String keyword, Long roleId, Long departmentId, Pageable pageable) {
        Specification<User> spec = UserSpecifications.filterUsers(keyword, roleId, departmentId);

        // Bước 1: Query phân trang lấy raw page (không fetch collection)
        Page<User> rawPage = userRepository.findAll(spec, pageable);
        if (rawPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Bước 2: Nạp chi tiết quan hệ cho đúng tập 10-20 IDs của trang đó
        List<Long> ids = rawPage.getContent().stream().map(User::getId).toList();
        List<User> fullUsers = userRepository.findAllByIdsWithRolesAndDepartment(ids);

        // Giữ nguyên đúng thứ tự sắp xếp của rawPage
        Map<Long, User> userMap = fullUsers.stream().collect(Collectors.toMap(User::getId, u -> u));
        List<User> orderedUsers = ids.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(orderedUsers, pageable, rawPage.getTotalElements());
    }

    /**
     * Lấy danh sách Giảng viên hoạt động phục vụ phân công / tạo đề tài (fetch sẵn roles).
     */
    @Transactional(readOnly = true)
    public List<User> getActiveLecturers(Long excludeUserId) {
        return userRepository.findActiveLecturers(excludeUserId);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));
    }

    /**
     * Tạo tài khoản người dùng mới có kiểm soát phân quyền (Privilege Escalation Prevention).
     */
    @Transactional
    public User createUser(UserCreateDto dto, User currentAdmin) {
        if (dto.getUserCode() == null || dto.getUserCode().trim().isEmpty() ||
            dto.getFullName() == null || dto.getFullName().trim().isEmpty() ||
            dto.getEmail() == null || dto.getEmail().trim().isEmpty() ||
            dto.getRoleId() == null) {
            throw new IllegalArgumentException("Vui lòng điền đầy đủ các thông tin bắt buộc!");
        }

        Role targetRole = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò với ID: " + dto.getRoleId()));

        // Kiểm soát nâng quyền: Chỉ tài khoản ROLE_ADMIN mới được tạo tài khoản có vai trò ROLE_ADMIN
        if ("ROLE_ADMIN".equalsIgnoreCase(targetRole.getRoleName()) && 
                (currentAdmin == null || !currentAdmin.hasRole("ROLE_ADMIN"))) {
            throw new SecurityException("Vi phạm phân quyền: Chỉ Quản trị viên hệ thống mới có quyền cấp tài khoản ADMIN!");
        }

        String cleanCode = dto.getUserCode().trim().toUpperCase();
        String cleanEmail = dto.getEmail().trim().toLowerCase();

        if (userRepository.existsByUserCode(cleanCode)) {
            throw new IllegalStateException("Mã người dùng [" + cleanCode + "] đã tồn tại!");
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalStateException("Email [" + cleanEmail + "] đã được sử dụng!");
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId()).orElse(null);
        }

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) 
                ? dto.getPassword().trim() : "123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Set<Role> roles = new HashSet<>();
        roles.add(targetRole);

        User newUser = User.builder()
                .userCode(cleanCode)
                .fullName(dto.getFullName().trim())
                .email(cleanEmail)
                .phone(dto.getPhone() != null && !dto.getPhone().isBlank() ? dto.getPhone().trim() : null)
                .password(encodedPassword)
                .department(department)
                .academicRank(dto.getAcademicRank() != null && !dto.getAcademicRank().isBlank() ? dto.getAcademicRank().trim() : null)
                .className(dto.getClassName() != null && !dto.getClassName().isBlank() ? dto.getClassName().trim() : null)
                .isActive(true)
                .roles(roles)
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Bật/Tắt trạng thái hoạt động của tài khoản.
     * Khi khóa (isActive = false), lập tức hủy toàn bộ phiên đăng nhập của người dùng qua SessionRegistry.
     */
    @Transactional
    public boolean toggleUserStatus(Long userId, User currentAdmin) {
        User user = getUserById(userId);

        if (currentAdmin != null && user.getId().equals(currentAdmin.getId())) {
            throw new IllegalStateException("Không thể tự vô hiệu hóa tài khoản quản trị của chính bạn!");
        }

        boolean newStatus = !Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(newStatus);
        userRepository.save(user);

        // NẾU KHÓA TÀI KHOẢN: Hủy phiên làm việc ngay lập tức
        if (!newStatus && sessionRegistry != null) {
            expireUserSessions(user);
        }

        return newStatus;
    }

    private void expireUserSessions(User user) {
        try {
            for (Object principal : sessionRegistry.getAllPrincipals()) {
                if (principal instanceof UserDetails ud) {
                    // So khớp an toàn cả userCode lẫn email
                    boolean matchUserCode = ud.getUsername() != null && ud.getUsername().equalsIgnoreCase(user.getUserCode());
                    boolean matchEmail = user.getEmail() != null && ud.getUsername() != null && ud.getUsername().equalsIgnoreCase(user.getEmail());

                    if (matchUserCode || matchEmail) {
                        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                        for (SessionInformation session : sessions) {
                            session.expireNow();
                            log.info("Expired active session for locked user [{}]", user.getUserCode());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể hủy phiên đăng nhập cho user [{}]: {}", user.getUserCode(), e.getMessage());
        }
    }
}
