package com.quanlydetai.controller;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.dto.UserCreateDto;
import com.quanlydetai.entity.Department;
import com.quanlydetai.entity.Role;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.DepartmentRepository;
import com.quanlydetai.repository.RoleRepository;
import com.quanlydetai.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(size = 15, sort = "userCode", direction = Sort.Direction.ASC) Pageable pageable,
            Model model) {

        // Tìm kiếm và phân trang Two-phase tối ưu dưới Database Engine, triệt tiêu N+1 và HHH000104
        Page<User> usersPage = userService.searchUsers(keyword, roleId, departmentId, pageable);

        List<Role> roles = roleRepository.findAll();
        List<Department> departments = departmentRepository.findAll();

        model.addAttribute("users", usersPage);
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        UserCreateDto dto = UserCreateDto.builder()
                .userCode(userCode)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .roleId(roleId)
                .departmentId(departmentId)
                .academicRank(academicRank)
                .className(className)
                .password(password)
                .build();

        try {
            User newUser = userService.createUser(dto, userDetails.getUser());
            redirectAttributes.addFlashAttribute("successMessage", 
                    "Tạo tài khoản người dùng [" + newUser.getUserCode() + "] thành công!");
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        try {
            boolean newStatus = userService.toggleUserStatus(id, userDetails.getUser());
            User targetUser = userService.getUserById(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    (newStatus ? "Đã kích hoạt" : "Đã khóa") + " tài khoản [" + targetUser.getUserCode() + "] thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", com.quanlydetai.util.SqlErrorUtils.extractFriendlyMessage(e));
        }

        return "redirect:/admin/users";
    }
}
