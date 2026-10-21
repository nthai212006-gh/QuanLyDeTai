package com.quanlydetai.service;

import com.quanlydetai.dto.UserCreateDto;
import com.quanlydetai.entity.Department;
import com.quanlydetai.entity.Role;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.DepartmentRepository;
import com.quanlydetai.repository.RoleRepository;
import com.quanlydetai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private User adminActor;
    private Role studentRole;
    private Role adminRole;
    private Department sampleDepartment;

    @BeforeEach
    void setUp() {
        studentRole = new Role();
        studentRole.setId(1L);
        studentRole.setRoleName("ROLE_STUDENT");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setRoleName("ROLE_ADMIN");

        sampleDepartment = new Department();
        sampleDepartment.setId(1L);
        sampleDepartment.setDeptName("Công nghệ thông tin");

        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setUserCode("SV001");
        sampleUser.setEmail("sv001@school.edu.vn");
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setIsActive(true);
        sampleUser.setRoles(new HashSet<>(Collections.singletonList(studentRole)));
        sampleUser.setDepartment(sampleDepartment);

        adminActor = new User();
        adminActor.setId(1L);
        adminActor.setUserCode("ADMIN01");
        adminActor.setEmail("admin@school.edu.vn");
        adminActor.setIsActive(true);
        adminActor.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
    }

    @Test
    @DisplayName("Two-phase pagination: phase 1 gets IDs, phase 2 fetches eager graph without in-memory warning")
    void testSearchUsers_TwoPhase() {
        Pageable pageable = PageRequest.of(0, 10);
        User idOnlyUser = new User();
        idOnlyUser.setId(10L);
        Page<User> idPage = new PageImpl<>(Collections.singletonList(idOnlyUser), pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(idPage);
        when(userRepository.findAllByIdsWithRolesAndDepartment(Collections.singletonList(10L)))
                .thenReturn(Collections.singletonList(sampleUser));

        Page<User> result = userService.searchUsers("SV", 1L, 1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserCode()).isEqualTo("SV001");

        verify(userRepository, times(1)).findAll(any(Specification.class), eq(pageable));
        verify(userRepository, times(1)).findAllByIdsWithRolesAndDepartment(Collections.singletonList(10L));
    }

    @Test
    @DisplayName("Create user: prevent privilege escalation if non-admin tries to assign ROLE_ADMIN")
    void testCreateUser_PreventPrivilegeEscalation() {
        UserCreateDto dto = new UserCreateDto();
        dto.setUserCode("ADMIN_NEW");
        dto.setEmail("admin_new@school.edu.vn");
        dto.setPassword("123456");
        dto.setFullName("Fake Admin");
        dto.setRoleId(2L); // ADMIN role ID

        when(roleRepository.findById(2L)).thenReturn(Optional.of(adminRole));

        // Non-admin actor (sampleUser has ROLE_STUDENT)
        assertThatThrownBy(() -> userService.createUser(dto, sampleUser))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Chỉ Quản trị viên hệ thống mới có quyền cấp tài khoản ADMIN");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Create user: success with ROLE_STUDENT")
    void testCreateUser_Success() {
        UserCreateDto dto = new UserCreateDto();
        dto.setUserCode("SV002");
        dto.setEmail("sv002@school.edu.vn");
        dto.setPassword("123456");
        dto.setFullName("Tran Van B");
        dto.setRoleId(1L);
        dto.setDepartmentId(1L);

        when(userRepository.existsByUserCode("SV002")).thenReturn(false);
        when(userRepository.existsByEmail("sv002@school.edu.vn")).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(studentRole));
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(sampleDepartment));
        when(passwordEncoder.encode("123456")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User created = userService.createUser(dto, adminActor);

        assertThat(created).isNotNull();
        assertThat(created.getUserCode()).isEqualTo("SV002");
        assertThat(created.getPassword()).isEqualTo("hashedPassword");
        assertThat(created.getRoles()).contains(studentRole);
    }

    @Test
    @DisplayName("Toggle user status: invalidates sessions in SessionRegistry when locking account")
    void testToggleUserStatus_InvalidateSessions() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("SV001");

        SessionInformation sessionInfo = mock(SessionInformation.class);
        when(sessionRegistry.getAllPrincipals()).thenReturn(Collections.singletonList(mockUserDetails));
        when(sessionRegistry.getAllSessions(mockUserDetails, false)).thenReturn(Collections.singletonList(sessionInfo));

        boolean newStatus = userService.toggleUserStatus(10L, adminActor);

        assertThat(newStatus).isFalse();
        assertThat(sampleUser.getIsActive()).isFalse();
        verify(sessionInfo, times(1)).expireNow();
    }
}
