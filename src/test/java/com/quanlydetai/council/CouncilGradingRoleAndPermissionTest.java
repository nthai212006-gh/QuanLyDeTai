package com.quanlydetai.council;

import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
import com.quanlydetai.service.CouncilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CouncilGradingRoleAndPermissionTest {

    @Autowired private CouncilService councilService;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationPeriodRepository periodRepository;
    @Autowired private CouncilRepository councilRepository;
    @Autowired private CouncilMemberRepository memberRepository;
    @Autowired private CouncilTopicRepository councilTopicRepository;
    @Autowired private StudentGroupRepository groupRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private TopicSupervisorRepository supervisorRepository;
    @Autowired private DepartmentRepository departmentRepository;

    private Council council;
    private CouncilTopic councilTopic;
    private User chair;
    private User member;
    private User secretary;
    private User reviewer;
    private User supervisor;
    private User outsider;

    @BeforeEach
    void setUp() {
        List<User> lecturers = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER"))
                .toList();
        assertThat(lecturers.size()).isGreaterThanOrEqualTo(6);

        chair = lecturers.get(0);
        member = lecturers.get(1);
        secretary = lecturers.get(2);
        reviewer = lecturers.get(3);
        supervisor = lecturers.get(4);
        outsider = lecturers.get(5);

        Department dept = departmentRepository.findAll().get(0);

        RegistrationPeriod period = RegistrationPeriod.builder()
                .periodName("Grading Test Period")
                .periodType(RegistrationPeriod.PeriodType.GRADUATION_THESIS)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(LocalDateTime.now().minusDays(10))
                .topicSubmissionEnd(LocalDateTime.now().minusDays(5))
                .studentRegistrationStart(LocalDateTime.now().minusDays(4))
                .studentRegistrationEnd(LocalDateTime.now().plusDays(5))
                .reviewDeadline(LocalDateTime.now().plusDays(20))
                .defenseDate(LocalDateTime.now().plusDays(30).toLocalDate())
                .createdBy(chair)
                .build();
        periodRepository.save(period);

        council = Council.builder()
                .councilCode("HD_TEST_GRADE_" + System.currentTimeMillis())
                .councilName("Hoi Dong Test Grade")
                .period(period)
                .defenseDate(LocalDateTime.now().plusDays(30))
                .status(Council.CouncilStatus.IN_SESSION)
                .build();
        councilRepository.save(council);

        // Add 3 council members (CHAIR, MEMBER, SECRETARY)
        memberRepository.save(CouncilMember.builder().council(council).lecturer(chair).position(CouncilMember.CouncilPosition.CHAIR).build());
        memberRepository.save(CouncilMember.builder().council(council).lecturer(member).position(CouncilMember.CouncilPosition.MEMBER).build());
        memberRepository.save(CouncilMember.builder().council(council).lecturer(secretary).position(CouncilMember.CouncilPosition.SECRETARY).build());

        Topic topic = Topic.builder()
                .topicCode("TP_TEST_" + System.currentTimeMillis())
                .title("De tai test grading")
                .description("Mo ta")
                .period(period)
                .department(dept)
                .createdByLecturer(supervisor)
                .status(Topic.TopicStatus.APPROVED)
                .build();
        topicRepository.save(topic);

        supervisorRepository.save(TopicSupervisor.builder()
                .topic(topic)
                .lecturer(supervisor)
                .isPrimary(true)
                .build());

        User student = userRepository.findAll().stream().filter(u -> u.hasRole("ROLE_STUDENT")).findFirst().orElseThrow();
        StudentGroup group = StudentGroup.builder()
                .groupName("Nhom Test Grading")
                .period(period)
                .leader(student)
                .topic(topic)
                .status(StudentGroup.GroupStatus.DEFENDING)
                .build();
        groupRepository.save(group);

        councilTopic = CouncilTopic.builder()
                .council(council)
                .group(group)
                .reviewerLecturer(reviewer)
                .defenseOrder(1)
                .isPublished(false)
                .build();
        councilTopicRepository.save(councilTopic);
    }

    @Test
    @DisplayName("FB6: Chu tich cham diem voi evalType null -> tu suy COUNCIL_CHAIR")
    void testGradeTopic_ChairAutoInfersCouncilChair() {
        Evaluation eval = councilService.gradeTopic(
                councilTopic.getId(), chair, null,
                new BigDecimal("8.5"), new BigDecimal("9.0"), new BigDecimal("8.0"), "Tot"
        );

        assertThat(eval.getEvaluationType()).isEqualTo(Evaluation.EvaluationType.COUNCIL_CHAIR);
        assertThat(eval.getTotalScore()).isNotNull();
    }

    @Test
    @DisplayName("FB6: Uy vien cham diem voi evalType null -> tu suy COUNCIL_MEMBER")
    void testGradeTopic_MemberAutoInfersCouncilMember() {
        Evaluation eval = councilService.gradeTopic(
                councilTopic.getId(), member, null,
                new BigDecimal("8.0"), new BigDecimal("8.0"), new BigDecimal("8.0"), "Tot"
        );

        assertThat(eval.getEvaluationType()).isEqualTo(Evaluation.EvaluationType.COUNCIL_MEMBER);
    }

    @Test
    @DisplayName("FB6: GVPB cham diem voi evalType null -> tu suy REVIEWER")
    void testGradeTopic_ReviewerAutoInfersReviewer() {
        Evaluation eval = councilService.gradeTopic(
                councilTopic.getId(), reviewer, null,
                new BigDecimal("7.5"), new BigDecimal("8.0"), new BigDecimal("7.0"), "Nhan xet"
        );

        assertThat(eval.getEvaluationType()).isEqualTo(Evaluation.EvaluationType.REVIEWER);
    }

    @Test
    @DisplayName("D2: GVHD cham de tai cua minh -> bi chan (IllegalStateException)")
    void testGradeTopic_SupervisorBlocked() {
        assertThatThrownBy(() -> councilService.gradeTopic(
                councilTopic.getId(), supervisor, null,
                new BigDecimal("9.0"), new BigDecimal("9.0"), new BigDecimal("9.0"), "Tu cham"
        ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("không được chấm đề tài");
    }

    @Test
    @DisplayName("D2: GV khong thuoc hoi dong va khong phai GVPB -> bi chan (IllegalArgumentException)")
    void testGradeTopic_OutsiderBlocked() {
        assertThatThrownBy(() -> councilService.gradeTopic(
                councilTopic.getId(), outsider, null,
                new BigDecimal("8.0"), new BigDecimal("8.0"), new BigDecimal("8.0"), "Cham ngoai"
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Bạn không thuộc hội đồng này hoặc không phải GVPB");
    }
}
