package com.quanlydetai.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Dean TK001 login successfully with password 123456")
    void testDeanLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "TK001")
                .param("password", "123456")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Lecturer GV001 login successfully with password 123456")
    void testLecturerLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "GV001")
                .param("password", "123456")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Student SV001 login successfully with password 123456")
    void testStudentLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "SV001")
                .param("password", "123456")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Admin ADMIN01 login successfully with password 123456")
    void testAdminLoginSuccess() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "ADMIN01")
                .param("password", "123456")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Login failure with incorrect password")
    void testLoginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "TK001")
                .param("password", "wrong_password")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }
}