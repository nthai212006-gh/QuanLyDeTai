package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_code", unique = true, nullable = false, length = 50)
    private String userCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Transient
    private Gender gender;

    @Transient
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "academic_rank", length = 50)
    private String academicRank;

    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getRoleName().equalsIgnoreCase(roleName));
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, LOCKED
    }

    @Transient
    public UserStatus getStatus() {
        return Boolean.TRUE.equals(isActive) ? UserStatus.ACTIVE : UserStatus.LOCKED;
    }

    public void setStatus(UserStatus status) {
        this.isActive = (status == UserStatus.ACTIVE);
    }

    private static final java.util.Locale VI_LOCALE = java.util.Locale.forLanguageTag("vi-VN");

    /**
     * Trích xuất 2 chữ cái viết hoa đại diện cho người dùng (Monogram Avatar)
     * Quy tắc: Chữ cái đầu của Họ + Chữ cái đầu của Tên (VD: "Nguyễn Trung Hải" -> "NH")
     */
    @Transient
    public String getInitials() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split("\\s+");
            if (parts.length >= 2) {
                String firstWord = parts[0];
                String lastWord = parts[parts.length - 1];
                if (!firstWord.isEmpty() && !lastWord.isEmpty()) {
                    return (firstWord.substring(0, 1) + lastWord.substring(0, 1)).toUpperCase(VI_LOCALE);
                }
            } else if (parts.length == 1 && !parts[0].isEmpty()) {
                String single = parts[0];
                return (single.length() >= 2 ? single.substring(0, 2) : single).toUpperCase(VI_LOCALE);
            }
        }

        // Fallback cấp 2: Dựa vào mã tài khoản userCode (VD: "SV001" -> "SV")
        if (userCode != null && !userCode.trim().isEmpty()) {
            String cleanCode = userCode.trim();
            return (cleanCode.length() >= 2 ? cleanCode.substring(0, 2) : cleanCode).toUpperCase(VI_LOCALE);
        }

        // Fallback cấp 3: Mặc định an toàn
        return "??";
    }
}
