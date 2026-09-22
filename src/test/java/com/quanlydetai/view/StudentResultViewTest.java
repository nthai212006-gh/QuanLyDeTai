package com.quanlydetai.view;

import com.quanlydetai.entity.*;
import com.quanlydetai.entity.RegistrationPeriod.PeriodType;
import com.quanlydetai.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD - Phase 4 - Student Result View
 * Seams:
 *   1. CouncilTopicRepository.findByGroupId returns published result
 *   2. Controller logic: filter(isPublished) -> model attribute
 *   3. CouncilTopic data integrity (finalCouncilScore, evaluations)
 */
@SpringBootTest
@Transactional
class StudentResultViewTest {

    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationPeriodRepository periodRepository;
    @Autowired private StudentGroupRepository groupRepository;
    @Autowired private GroupMemberRepository memberRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private CouncilRepository councilRepository;
    @Autowired private CouncilTopicRepository councilTopicRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private EvaluationRepository evaluationRepository;

    private User student;
    private StudentGroup group;
    private CouncilTopic councilTopic;
    private RegistrationPeriod period;

    @BeforeEach
    void setUp() {
        student = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_STUDENT"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No STUDENT in DB"));

        User lecturer = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No LECTURER in DB"));

        Department dept = departmentRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No department"));

        period = RegistrationPeriod.builder()
                .periodName("Result View Test Period")
                .periodType(PeriodType.GRADUATION_THESIS)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(LocalDateTime.now().minusDays(30))
                .topicSubmissionEnd(LocalDateTime.now().minusDays(20))
                .studentRegistrationStart(LocalDateTime.now().minusDays(19))
                .studentRegistrationEnd(LocalDateTime.now().minusDays(10))
                .createdBy(lecturer)
                .build();
        periodRepository.save(period);

        Topic topic = Topic.builder()
                .topicCode("TC_RESULT_01")
                .title("Result Test Topic")
                .description("desc")
                .department(dept)
                .period(period)
                .createdByLecturer(lecturer)
                .status(Topic.TopicStatus.ASSIGNED)
                .build();
        topicRepository.save(topic);

        group = StudentGroup.builder()
                .groupCode("GRP_RESULT")
                .groupName("Result Test Group")
                .period(period)
                .leader(student)
                .topic(topic)
                .status(StudentGroup.GroupStatus.COMPLETED)
                .registrationStatus(StudentGroup.RegistrationStatus.APPROVED)
                .finalScore(new BigDecimal("8.50"))
                .build();
        groupRepository.save(group);

        GroupMember leaderMember = GroupMember.builder()
                .group(group)
                .student(student)
                .period(period)
                .roleInGroup(GroupMember.RoleInGroup.LEADER)
                .build();
        memberRepository.save(leaderMember);

        Council council = Council.builder()
                .councilCode("COUNCIL_R")
                .councilName("Result Council")
                .period(period)
                .defenseDate(LocalDateTime.now().minusDays(5))
                .location("Phong B301")
                .status(Council.CouncilStatus.COMPLETED)
                .build();
        councilRepository.save(council);

        councilTopic = CouncilTopic.builder()
                .council(council)
                .group(group)
                .defenseOrder(1)
                .isPublished(true)
                .finalCouncilScore(new BigDecimal("8.50"))
                .publishedAt(LocalDateTime.now().minusDays(3))
                .build();
        councilTopicRepository.save(councilTopic);
    }

    // SEAM 1: Repository tra ve councilTopic dung nhom
    @Test
    void findByGroupId_ReturnsCouncilTopicForGroup() {
        CouncilTopic ct = councilTopicRepository.findByGroupId(group.getId()).orElseThrow();
        assertThat(ct.getId()).isEqualTo(councilTopic.getId());
        assertThat(ct.getGroup().getId()).isEqualTo(group.getId());
    }

    // SEAM 2: isPublished = true -> controller filter passes -> model nhan gia tri
    @Test
    void councilTopic_IsPublished_FilterSucceeds() {
        CouncilTopic ct = councilTopicRepository.findByGroupId(group.getId())
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                .orElseThrow();
        assertThat(ct.getIsPublished()).isTrue();
    }

    // SEAM 3: Khi chua published -> filter tra ve empty -> model khong co councilTopic
    @Test
    void councilTopic_NotPublished_FilterReturnsEmpty() {
        councilTopic.setIsPublished(false);
        councilTopicRepository.save(councilTopic);

        boolean hasPublished = councilTopicRepository.findByGroupId(group.getId())
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                .isPresent();
        assertThat(hasPublished).isFalse();
    }

    // SEAM 4: finalCouncilScore dung gia tri
    @Test
    void councilTopic_HasCorrectFinalScore() {
        CouncilTopic ct = councilTopicRepository.findByGroupId(group.getId()).orElseThrow();
        assertThat(ct.getFinalCouncilScore()).isEqualByComparingTo("8.50");
    }

    // SEAM 5: evaluations co the query duoc tu councilTopic
    @Test
    void councilTopic_WithEvaluation_EvaluationStoredCorrectly() {
        User evaluator = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER"))
                .findFirst().orElseThrow();

        Evaluation eval = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(evaluator)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_MEMBER)
                .criteria1Score(new BigDecimal("8"))
                .criteria2Score(new BigDecimal("9"))
                .criteria3Score(new BigDecimal("8"))
                .totalScore(new BigDecimal("8.50"))
                .comments("Tot")
                .gradedAt(LocalDateTime.now())
                .build();
        evaluationRepository.save(eval);

        long count = evaluationRepository.findByCouncilTopicId(councilTopic.getId()).size();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}