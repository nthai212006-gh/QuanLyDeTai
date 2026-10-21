package com.quanlydetai.auth;

import com.quanlydetai.config.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AuthenticationLazyLoadingIntegrationTest {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Task 0.4: loadUserByUsername by userCode fetches roles and getAuthorities works outside transaction without LazyInitializationException")
    void testLoadByUserCodeAndGetAuthoritiesOutsideSession() {
        // Gọi loadUserByUsername không có @Transactional bọc ngoài
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("TK001");
        assertNotNull(userDetails, "UserDetails không được null");

        // Gọi getAuthorities() khi Hibernate session đã đóng
        assertDoesNotThrow(() -> {
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            assertFalse(authorities.isEmpty(), "Danh sách vai trò không được rỗng");
            boolean hasDeanRole = authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEAN"));
            assertTrue(hasDeanRole, "Người dùng TK001 phải có quyền ROLE_DEAN");
        }, "Gọi getAuthorities() không được văng LazyInitializationException");
    }

    @Test
    @DisplayName("Task 0.4: loadUserByUsername by Email case-insensitive fetches roles cleanly")
    void testLoadByEmailAndGetAuthoritiesOutsideSession() {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("truongkhoa@fit.edu.vn");
        assertNotNull(userDetails);

        assertDoesNotThrow(() -> {
            Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
            assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_DEAN")));
        });
    }
}
