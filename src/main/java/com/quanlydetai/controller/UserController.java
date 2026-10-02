package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.Department;
import com.quanlydetai.entity.Role;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.DepartmentRepository;
import com.quanlydetai.repository.RoleRepository;
import com.quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long departmentId,
            Model model) {

        List<User> users = userRepository.findAll();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getUserCode() != null && u.getUserCode().toLowerCase().contains(kw))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (roleId != null) {
            users = users.stream()
                    .filter(u -> u.getRoles() != null && u.getRoles().stream().anyMatch(r -> r.getId().equals(roleId)))
                    .collect(Collectors.toList());
        }

        if (departmentId != null) {
            users = users.stream()
                    .filter(u -> u.getDepartment() != null && u.getDepartment().getId().equals(departmentId))
                    .collect(Collectors.toList());
        }

        List<Role> roles = roleRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        model.addAttribute("users", users);
        model.addAttribute("roles", roles);
        model.addAttribute("departments", departments);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRoleId", roleId);
        model.addAttribute("selectedDeptId", departmentId);

        return "admin/users";
    }

    @PostMapping("/create")
    public String createUser(
            @RequestParam("userCode") String userCode,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam("roleId") Long roleId,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "academicRank", required = false) String academicRank,
            @RequestParam(value = "className", required = false) String className,
            @RequestParam(value = "password", required = false) String password,
            RedirectAttributes redirectAttributes) {

        if (userCode == null || userCode.trim().isEmpty() ||
            fullName == null || fullName.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            roleId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng điền đầy đủ các thông tin bắt buộc!");
            return "redirect:/admin/users";
        }

        String cleanCode = userCode.trim().toUpperCase();
        String cleanEmail = email.trim().toLowerCase();

        if (userRepository.existsByUserCode(cleanCode)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mã người dùng [" + cleanCode + "] đã tồn tại!");
            return "redirect:/admin/users";
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email [" + cleanEmail + "] đã được sử dụng!");
            return "redirect:/admin/users";
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò với ID: " + roleId));

        Department department = null;
        if (departmentId != null) {
            department = departmentRepository.findById(departmentId).orElse(null);
        }

        String rawPassword = (password != null && !password.trim().isEmpty()) ? password.trim() : "123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User newUser = User.builder()
                .userCode(cleanCode)
                .fullName(fullName.trim())
                .email(cleanEmail)
                .phone(phone != null && !phone.isBlank() ? phone.trim() : null)
                .password(encodedPassword)
                .department(department)
                .academicRank(academicRank != null && !academicRank.isBlank() ? academicRank.trim() : null)
                .className(className != null && !className.isBlank() ? className.trim() : null)
                .isActive(true)
                .roles(roles)
                .build();

        userRepository.save(newUser);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản người dùng [" + cleanCode + "] thành công!");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + id));

        if (userDetails != null && user.getId().equals(userDetails.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tự vô hiệu hóa tài khoản quản trị của chính bạn!");
            return "redirect:/admin/users";
        }

        boolean newStatus = !Boolean.TRUE.equals(user.getIsActive());
        user.setIsActive(newStatus);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("successMessage",
                (newStatus ? "Đã kích hoạt" : "Đã khóa") + " tài khoản [" + user.getUserCode() + "] thành công!");
        return "redirect:/admin/users";
    }
}
