package com.quanlydetai.phase;

import com.quanlydetai.config.CustomUserDetails;
import com.quanlydetai.entity.Department;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.Topic;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.DepartmentRepository;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.repository.TopicRepository;
import com.quanlydetai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TopicControllerCreateExceptionHandlingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationPeriodRepository periodRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TopicRepository topicRepository;

    private User lecturer;
    private Department dept;

    @BeforeEach
    void setUp() {
        lecturer = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER"))
                .findFirst()
                .orElseThrow();
        dept = departmentRepository.findAll().get(0);
    }

    @Test
    @DisplayName("FB1: POST /topics/create voi dot da het han -> Redirect /topics/create va flash errorMessage, KHONG bi 500")
    void testCreateTopic_ExpiredPeriod_RedirectsWithErrorMessage() throws Exception {
        RegistrationPeriod expiredPeriod = RegistrationPeriod.builder()
                .periodName("Expired Period Test")
                .periodType(RegistrationPeriod.PeriodType.COURSE_PROJECT)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(LocalDateTime.now().minusDays(20))
                .topicSubmissionEnd(LocalDateTime.now().minusDays(10))
                .studentRegistrationStart(LocalDateTime.now().minusDays(5))
                .studentRegistrationEnd(LocalDateTime.now().plusDays(5))
                .createdBy(lecturer)
                .build();
        periodRepository.save(expiredPeriod);

        mockMvc.perform(post("/topics/create")
                .with(csrf())
                .with(user(new CustomUserDetails(lecturer)))
                .param("periodId", expiredPeriod.getId().toString())
                .param("departmentId", dept.getId().toString())
                .param("topicCode", "TC_EXPIRED_" + System.currentTimeMillis())
                .param("title", "De tai dot het han")
                .param("description", "Mo ta de tai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/topics/create"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("topic"));
    }

    @Test
    @DisplayName("FB1: POST /topics/create voi ma de tai trung lap -> Redirect /topics/create va flash thong bao than thien")
    void testCreateTopic_DuplicateTopicCode_RedirectsWithFriendlyDuplicateMessage() throws Exception {
        RegistrationPeriod openPeriod = RegistrationPeriod.builder()
                .periodName("Open Period Test")
                .periodType(RegistrationPeriod.PeriodType.COURSE_PROJECT)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(LocalDateTime.now().minusDays(2))
                .topicSubmissionEnd(LocalDateTime.now().plusDays(10))
                .studentRegistrationStart(LocalDateTime.now().plusDays(11))
                .studentRegistrationEnd(LocalDateTime.now().plusDays(20))
                .createdBy(lecturer)
                .build();
        periodRepository.save(openPeriod);

        String duplicateCode = "DUP_" + System.currentTimeMillis();

        // Tao de tai dau tien
        Topic existingTopic = Topic.builder()
                .topicCode(duplicateCode)
                .title("De tai goc")
                .description("Mo ta goc")
                .period(openPeriod)
                .department(dept)
                .createdByLecturer(lecturer)
                .status(Topic.TopicStatus.PENDING_APPROVAL)
                .build();
        topicRepository.saveAndFlush(existingTopic);

        // Thu tao de tai thu 2 cung ma
        mockMvc.perform(post("/topics/create")
                .with(csrf())
                .with(user(new CustomUserDetails(lecturer)))
                .param("periodId", openPeriod.getId().toString())
                .param("departmentId", dept.getId().toString())
                .param("topicCode", duplicateCode)
                .param("title", "De tai trung ma")
                .param("description", "Mo ta de tai trung"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/topics/create"))
                .andExpect(flash().attribute("errorMessage", "Mã đề tài đã tồn tại trong hệ thống. Vui lòng nhập mã đề tài khác!"))
                .andExpect(flash().attributeExists("topic"));
    }

    @Test
    @DisplayName("FB1: POST /topics/create hop le -> Redirect /topics voi flash successMessage")
    void testCreateTopic_ValidOpenPeriod_RedirectsToTopicsListWithSuccess() throws Exception {
        RegistrationPeriod openPeriod = RegistrationPeriod.builder()
                .periodName("Open Period Test 2")
                .periodType(RegistrationPeriod.PeriodType.COURSE_PROJECT)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(LocalDateTime.now().minusDays(2))
                .topicSubmissionEnd(LocalDateTime.now().plusDays(10))
                .studentRegistrationStart(LocalDateTime.now().plusDays(11))
                .studentRegistrationEnd(LocalDateTime.now().plusDays(20))
                .createdBy(lecturer)
                .build();
        periodRepository.save(openPeriod);

        mockMvc.perform(post("/topics/create")
                .with(csrf())
                .with(user(new CustomUserDetails(lecturer)))
                .param("periodId", openPeriod.getId().toString())
                .param("departmentId", dept.getId().toString())
                .param("topicCode", "VALID_" + System.currentTimeMillis())
                .param("title", "De tai hop le")
                .param("description", "Mo ta hop le"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/topics?periodId=" + openPeriod.getId()))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
