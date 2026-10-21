package com.quanlydetai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", unique = true, nullable = false, length = 50)
    private String roleName;

    @Column(length = 255)
    private String description;

    @Transient
    private LocalDateTime createdAt;

    /**
     * Tên hiển thị tiếng Việt ngắn gọn cố định cho vai trò trên giao diện người dùng.
     * Lưu ý phân biệt:
     * - displayName: Nhãn ngắn gọn hiển thị UI (Sinh viên, Giảng viên...).
     * - description: Mô tả quyền hạn chi tiết lưu trong DB (được nạp từ seed data).
     * 
     * Gắn @JsonIgnore để bảo vệ tuyệt đối API contract, ngăn Jackson tự ý serialize field này ra REST API.
     */
    @JsonIgnore
    public String getDisplayName() {
        if (roleName == null) return "";
        return switch (roleName) {
            case "ROLE_ADMIN" -> "Quản trị viên";
            case "ROLE_DEAN" -> "Trưởng khoa";
            case "ROLE_HEAD_OF_DEPT" -> "Trưởng bộ môn";
            case "ROLE_LECTURER" -> "Giảng viên";
            case "ROLE_STUDENT" -> "Sinh viên";
            default -> roleName.replace("ROLE_", "");
        };
    }

    /**
     * Đóng gói tập trung CSS class màu sắc badge của vai trò theo chuẩn Bootstrap 5.
     * Triệt tiêu toàn bộ nested ternary phức tạp trong Thymeleaf template (Đảm bảo nguyên tắc DRY).
     * 
     * Gắn @JsonIgnore để bảo vệ API contract.
     */
    @JsonIgnore
    public String getBadgeCssClass() {
        if (roleName == null) return "bg-secondary text-white";
        return switch (roleName) {
            case "ROLE_ADMIN" -> "bg-danger text-white";
            case "ROLE_DEAN" -> "bg-warning text-dark";
            case "ROLE_HEAD_OF_DEPT" -> "bg-info text-dark";
            case "ROLE_LECTURER" -> "bg-primary text-white";
            default -> "bg-secondary text-white";
        };
    }
}
