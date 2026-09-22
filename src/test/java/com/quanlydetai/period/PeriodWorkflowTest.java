package com.quanlydetai.period;

import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.RegistrationPeriod.PeriodType;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.UserRepository;
import com.quanlydetai.service.PeriodService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PeriodWorkflowTest {

    @Autowired
    private PeriodService periodService;

    @Autowired
    private UserRepository userRepository;

    private User anyUser() {
        return userRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No user in DB"));
    }

    private RegistrationPeriod basePeriod(PeriodType type) {
        LocalDateTime now = LocalDateTime.now();
        return RegistrationPeriod.builder()
                .periodName("Test Period")
                .periodType(type)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(now.minusDays(10))
                .topicSubmissionEnd(now.minusDays(5))
                .studentRegistrationStart(now.minusDays(4))
                .studentRegistrationEnd(now.plusDays(5))
                .build();
    }

    // SLICE 1: COURSE_PROJECT phai xoa reviewDeadline va defenseDate
    @Test
    void testCourseProject_ClearsConditionalDeadlines() {
        User creator = anyUser();
        RegistrationPeriod p = basePeriod(PeriodType.COURSE_PROJECT);
        p.setReviewDeadline(LocalDateTime.now().plusDays(20));
        p.setDefenseDate(LocalDateTime.now().plusDays(30).toLocalDate());

        RegistrationPeriod saved = periodService.createPeriod(p, creator);

        assertThat(saved.getReviewDeadline()).as("COURSE_PROJECT no reviewDeadline").isNull();
        assertThat(saved.getDefenseDate()).as("COURSE_PROJECT no defenseDate").isNull();
    }

    // SLICE 2: RESEARCH phai xoa ca hai ngay
    @Test
    void testResearch_ClearsConditionalDeadlines() {
        User creator = anyUser();
        RegistrationPeriod p = basePeriod(PeriodType.RESEARCH);
        p.setReviewDeadline(LocalDateTime.now().plusDays(20));
        p.setDefenseDate(LocalDateTime.now().plusDays(30).toLocalDate());

        RegistrationPeriod saved = periodService.createPeriod(p, creator);

        assertThat(saved.getReviewDeadline()).isNull();
        assertThat(saved.getDefenseDate()).isNull();
    }

    // SLICE 3: INTERNSHIP giu reviewDeadline, xoa defenseDate
    @Test
    void testInternship_ClearsDefenseDate() {
        User creator = anyUser();
        RegistrationPeriod p = basePeriod(PeriodType.INTERNSHIP);
        p.setReviewDeadline(LocalDateTime.now().plusDays(20));
        p.setDefenseDate(LocalDateTime.now().plusDays(30).toLocalDate());

        RegistrationPeriod saved = periodService.createPeriod(p, creator);

        assertThat(saved.getReviewDeadline()).as("INTERNSHIP allows reviewDeadline").isNotNull();
        assertThat(saved.getDefenseDate()).as("INTERNSHIP no defenseDate").isNull();
    }

    // SLICE 4: GRADUATION_THESIS giu ca hai ngay
    @Test
    void testGraduationThesis_AllowsBothDates() {
        User creator = anyUser();
        RegistrationPeriod p = basePeriod(PeriodType.GRADUATION_THESIS);
        p.setReviewDeadline(LocalDateTime.now().plusDays(20));
        p.setDefenseDate(LocalDateTime.now().plusDays(30).toLocalDate());

        RegistrationPeriod saved = periodService.createPeriod(p, creator);

        assertThat(saved.getReviewDeadline()).isNotNull();
        assertThat(saved.getDefenseDate()).isNotNull();
    }

    // SLICE 5: GV end truoc start phai throw
    @Test
    void testInvalidDateSequence_GvEndBeforeStart_ThrowsException() {
        User creator = anyUser();
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriod p = RegistrationPeriod.builder()
                .periodName("Bad Dates GV")
                .periodType(PeriodType.COURSE_PROJECT)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(now.plusDays(5))
                .topicSubmissionEnd(now.plusDays(1))
                .studentRegistrationStart(now.plusDays(6))
                .studentRegistrationEnd(now.plusDays(10))
                .build();

        assertThatThrownBy(() -> periodService.createPeriod(p, creator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GV");
    }

    // SLICE 6: SV end truoc start phai throw
    @Test
    void testInvalidDateSequence_SvEndBeforeStart_ThrowsException() {
        User creator = anyUser();
        LocalDateTime now = LocalDateTime.now();
        RegistrationPeriod p = RegistrationPeriod.builder()
                .periodName("Bad SV Dates")
                .periodType(PeriodType.COURSE_PROJECT)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(now.minusDays(10))
                .topicSubmissionEnd(now.minusDays(5))
                .studentRegistrationStart(now.plusDays(10))
                .studentRegistrationEnd(now.plusDays(3))
                .build();

        assertThatThrownBy(() -> periodService.createPeriod(p, creator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SV");
    }

    // SLICE 7: isTopicSubmissionOpen() dung trong cua so GV
    @Test
    void testIsTopicSubmissionOpen_WithinWindow_ReturnsTrue() {
        RegistrationPeriod p = basePeriod(PeriodType.COURSE_PROJECT);
        p.setTopicSubmissionStart(LocalDateTime.now().minusHours(1));
        p.setTopicSubmissionEnd(LocalDateTime.now().plusHours(1));
        assertThat(p.isTopicSubmissionOpen()).isTrue();
    }

    // SLICE 8: isStudentRegistrationOpen() dung trong cua so SV
    @Test
    void testIsStudentRegistrationOpen_WithinWindow_ReturnsTrue() {
        RegistrationPeriod p = basePeriod(PeriodType.COURSE_PROJECT);
        assertThat(p.isStudentRegistrationOpen()).isTrue();
    }

    // SLICE 9 (RED): Tu choi Hoc ky 3 (Ky He)
    @Test
    void testSemester3_SummerSemester_ThrowsException() {
        User creator = anyUser();
        RegistrationPeriod p = basePeriod(PeriodType.COURSE_PROJECT);
        p.setSemester(3); // Hoc ky He

        assertThatThrownBy(() -> periodService.createPeriod(p, creator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Học kỳ");
    }

    // SLICE 10: Chap nhan Hoc ky 1 va Hoc ky 2
    @Test
    void testValidSemesters_1And2_Succeed() {
        User creator = anyUser();
        RegistrationPeriod p1 = basePeriod(PeriodType.COURSE_PROJECT);
        p1.setSemester(1);
        RegistrationPeriod saved1 = periodService.createPeriod(p1, creator);
        assertThat(saved1.getSemester()).isEqualTo(1);

        RegistrationPeriod p2 = basePeriod(PeriodType.COURSE_PROJECT);
        p2.setSemester(2);
        RegistrationPeriod saved2 = periodService.createPeriod(p2, creator);
        assertThat(saved2.getSemester()).isEqualTo(2);
    }
}