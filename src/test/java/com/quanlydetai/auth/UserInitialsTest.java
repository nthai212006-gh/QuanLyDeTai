package com.quanlydetai.auth;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserInitialsTest {

    @Test
    @DisplayName("Chuẩn hóa tên tiếng Việt 3 từ -> Chữ cái đầu Họ + Tên: Nguyễn Trung Hải -> NH")
    void testStandardVietnameseName() {
        User user = User.builder()
                .userCode("SV001")
                .fullName("Nguyễn Trung Hải")
                .build();
        assertEquals("NH", user.getInitials());

        CustomUserDetails userDetails = new CustomUserDetails(user);
        assertEquals("NH", userDetails.getInitials());
        assertEquals("Nguyễn Trung Hải", userDetails.getFullName());
    }

    @Test
    @DisplayName("Xử lý khoảng trắng thừa/nhiều dấu cách: '  Trần   Văn   Nam  ' -> TN")
    void testExtraWhitespaceName() {
        User user = User.builder()
                .userCode("SV002")
                .fullName("  Trần   Văn   Nam  ")
                .build();
        assertEquals("TN", user.getInitials());
    }

    @Test
    @DisplayName("Tên 2 từ: 'Lê An' -> LA")
    void testTwoWordName() {
        User user = User.builder()
                .userCode("SV003")
                .fullName("Lê An")
                .build();
        assertEquals("LA", user.getInitials());
    }

    @Test
    @DisplayName("Tên 1 từ: 'Admin' -> AD, 'A' -> A")
    void testSingleWordName() {
        User user1 = User.builder()
                .userCode("AD001")
                .fullName("Admin")
                .build();
        assertEquals("AD", user1.getInitials());

        User user2 = User.builder()
                .userCode("AD002")
                .fullName("A")
                .build();
        assertEquals("A", user2.getInitials());
    }

    @Test
    @DisplayName("FullName null hoặc rỗng -> Fallback userCode: 'SV001' -> SV")
    void testFallbackToUserCode() {
        User user1 = User.builder()
                .userCode("SV001")
                .fullName(null)
                .build();
        assertEquals("SV", user1.getInitials());

        User user2 = User.builder()
                .userCode("TK001")
                .fullName("   ")
                .build();
        assertEquals("TK", user2.getInitials());
    }

    @Test
    @DisplayName("Cả FullName và UserCode đều null/rỗng -> Fallback an toàn '??'")
    void testExtremeEdgeCases() {
        User user1 = User.builder()
                .userCode(null)
                .fullName(null)
                .build();
        assertEquals("??", user1.getInitials());

        CustomUserDetails nullDetails = new CustomUserDetails(null);
        assertEquals("??", nullDetails.getInitials());
        assertNull(nullDetails.getFullName());
    }
}
