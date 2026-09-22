package com.quanlydetai.trigger;

import com.quanlydetai.entity.CouncilTopic;
import com.quanlydetai.entity.Evaluation;
import com.quanlydetai.entity.GroupMember;
import com.quanlydetai.entity.StudentGroup;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.CouncilTopicRepository;
import com.quanlydetai.repository.EvaluationRepository;
import com.quanlydetai.repository.GroupMemberRepository;
import com.quanlydetai.repository.StudentGroupRepository;
import com.quanlydetai.repository.UserRepository;
import com.quanlydetai.util.SqlErrorUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class DatabaseTriggerIntegrationTest {

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouncilTopicRepository councilTopicRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Test
    @DisplayName("Trigger trg_check_group_size should block 4th member and provide friendly Vietnamese message")
    @Transactional
    void testGroupSizeTriggerEnforcement() {
        // Group 1 already has 3 members (SV001, SV002, SV003) from seed data
        StudentGroup group1 = groupRepository.findById(1L).orElseThrow();
        User extraUser = userRepository.findById(1L).orElseThrow();

        GroupMember fourthMember = GroupMember.builder()
                .group(group1)
                .student(extraUser)
                .period(group1.getPeriod())
                .roleInGroup(GroupMember.RoleInGroup.MEMBER)
                .build();

        assertThatThrownBy(() -> {
            memberRepository.saveAndFlush(fourthMember);
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Quy định đề án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!");
        });
    }

    @Test
    @DisplayName("Trigger trg_prevent_supervisor_grading should block supervisor from grading their own topic")
    @Transactional
    void testSupervisorGradingTriggerEnforcement() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        // GV001 is supervisor of Topic 1 (Group 1)
        User supervisor = userRepository.findByUserCode("GV001").orElseThrow();

        Evaluation eval = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(supervisor)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_MEMBER)
                .criteria1Score(new BigDecimal("9.00"))
                .criteria2Score(new BigDecimal("9.00"))
                .criteria3Score(new BigDecimal("9.00"))
                .totalScore(new BigDecimal("9.00"))
                .build();

        assertThatThrownBy(() -> {
            evaluationRepository.saveAndFlush(eval);
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Lỗi vi phạm quy chế: Giảng viên không được phép chấm đề tài do chính mình đang hướng dẫn!");
        });
    }
}
