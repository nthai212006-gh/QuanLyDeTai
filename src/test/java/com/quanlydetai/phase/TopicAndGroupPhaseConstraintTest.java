package com.quanlydetai.phase;

import com.quanlydetai.entity.*;
import com.quanlydetai.entity.RegistrationPeriod.PeriodType;
import com.quanlydetai.repository.*;
import com.quanlydetai.service.GroupService;
import com.quanlydetai.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD - Phase 2 - 2-Phase Time Gate
 * Seam: TopicService.proposeTopic (GV phase guard)
 *       GroupService.registerTopic (SV phase guard)
 */
@SpringBootTest
@Transactional
class TopicAndGroupPhaseConstraintTest {

    @Autowired private TopicService topicService;
    @Autowired private GroupService groupService;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationPeriodRepository periodRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TopicRepository topicRepository;
    @Autowired private StudentGroupRepository groupRepository;
    @Autowired private GroupMemberRepository memberRepository;

    private User lecturer;
    private User student;
    private Department dept;

    @BeforeEach
    void setUp() {
        lecturer = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No LECTURER in DB"));

        student = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_STUDENT"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No STUDENT in DB"));

        dept = departmentRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No department in DB"));
    }

    private RegistrationPeriod savePeriod(LocalDateTime gvStart, LocalDateTime gvEnd,
                                          LocalDateTime svStart, LocalDateTime svEnd) {
        RegistrationPeriod p = RegistrationPeriod.builder()
                .periodName("Phase Test Period")
                .periodType(PeriodType.COURSE_PROJECT)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(gvStart)
                .topicSubmissionEnd(gvEnd)
                .studentRegistrationStart(svStart)
                .studentRegistrationEnd(svEnd)
                .createdBy(lecturer)
                .build();
        return periodRepository.save(p);
    }

    // ====================================================================
    // SEAM 1: TopicService.proposeTopic - GV phase guard
    // ====================================================================

    @Test
    void proposeTopic_OutsideGvWindow_ThrowsException() {
        // Cua so GV da dong (ket thuc qua khu)
        RegistrationPeriod period = savePeriod(
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5),  // da ket thuc
                LocalDateTime.now().minusDays(4),
                LocalDateTime.now().plusDays(5)
        );

        Topic topic = Topic.builder()
                .topicCode("TC_TEST_01")
                .title("Test Topic Out of Window")
                .description("desc")
                .build();

        assertThatThrownBy(() ->
                topicService.proposeTopic(topic, dept.getId(), period.getId(), lecturer, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GV");
    }

    @Test
    void proposeTopic_InsideGvWindow_Succeeds() {
        // Cua so GV dang mo
        RegistrationPeriod period = savePeriod(
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusDays(5),   // dang mo
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(10)
        );

        Topic topic = Topic.builder()
                .topicCode("TC_TEST_02")
                .title("Test Topic In Window")
                .description("desc")
                .build();

        Topic saved = topicService.proposeTopic(topic, dept.getId(), period.getId(), lecturer, null);
        assertThat(saved.getId()).isNotNull();
    }

    // ====================================================================
    // SEAM 2: GroupService.registerTopic - SV phase guard
    // ====================================================================

    @Test
    void registerTopic_OutsideSvWindow_ThrowsException() {
        // Cua so SV chua mo (con trong tuong lai)
        RegistrationPeriod period = savePeriod(
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().plusDays(3),   // SV chua bat dau
                LocalDateTime.now().plusDays(8)
        );

        // Tao de tai APPROVED trong period nay
        Topic topic = Topic.builder()
                .topicCode("TC_TEST_SV01")
                .title("Topic for SV test")
                .description("desc")
                .department(dept)
                .period(period)
                .createdByLecturer(lecturer)
                .status(Topic.TopicStatus.APPROVED)
                .build();
        topicRepository.save(topic);

        // Tao nhom
        StudentGroup group = StudentGroup.builder()
                .groupCode("GRP_SV_TEST")
                .groupName("SV Test Group")
                .period(period)
                .leader(student)
                .status(StudentGroup.GroupStatus.FORMING)
                .registrationStatus(StudentGroup.RegistrationStatus.NONE)
                .build();
        groupRepository.save(group);

        assertThatThrownBy(() ->
                groupService.registerTopic(group.getId(), topic.getId(), student))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SV");
    }

    @Test
    void registerTopic_InsideSvWindow_Succeeds() {
        // Cua so SV dang mo
        RegistrationPeriod period = savePeriod(
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusHours(1),  // SV dang mo
                LocalDateTime.now().plusDays(5)
        );

        Topic topic = Topic.builder()
                .topicCode("TC_TEST_SV02")
                .title("Topic for SV open test")
                .description("desc")
                .department(dept)
                .period(period)
                .createdByLecturer(lecturer)
                .status(Topic.TopicStatus.APPROVED)
                .build();
        topicRepository.save(topic);

        StudentGroup group = StudentGroup.builder()
                .groupCode("GRP_SV_OPEN")
                .groupName("SV Open Group")
                .period(period)
                .leader(student)
                .status(StudentGroup.GroupStatus.FORMING)
                .registrationStatus(StudentGroup.RegistrationStatus.NONE)
                .build();
        groupRepository.save(group);

        // Them student vao group_members de vuot qua check leader
        GroupMember leaderMember = GroupMember.builder()
                .group(group)
                .student(student)
                .period(period)
                .roleInGroup(GroupMember.RoleInGroup.LEADER)
                .build();
        memberRepository.save(leaderMember);

        groupService.registerTopic(group.getId(), topic.getId(), student);
        StudentGroup updated = groupRepository.findById(group.getId()).orElseThrow();
        assertThat(updated.getTopic()).isNotNull();
    }
}