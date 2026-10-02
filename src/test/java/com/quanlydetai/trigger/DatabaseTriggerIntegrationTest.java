package com.quanlydetai.trigger;

import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
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
    private CouncilRepository councilRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

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
    @DisplayName("Trigger trg_prevent_supervisor_grading should block primary supervisor from grading their own topic")
    @Transactional
    void testSupervisorGradingTriggerEnforcement() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        // GV001 is primary supervisor of Topic 1 (Group 1)
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

    @Test
    @DisplayName("TC_GRADE_CO_SUPERVISOR: Trigger trg_prevent_supervisor_grading should block co-supervisor from grading their topic")
    @Transactional
    void testCoSupervisorGradingTriggerEnforcement() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        // GV002 is co-supervisor of Topic 1 (is_primary = false)
        User coSupervisor = userRepository.findByUserCode("GV002").orElseThrow();

        Evaluation eval = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(coSupervisor)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_MEMBER)
                .criteria1Score(new BigDecimal("8.50"))
                .criteria2Score(new BigDecimal("8.50"))
                .criteria3Score(new BigDecimal("8.50"))
                .totalScore(new BigDecimal("8.50"))
                .build();

        assertThatThrownBy(() -> {
            evaluationRepository.saveAndFlush(eval);
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Lỗi vi phạm quy chế: Giảng viên không được phép chấm đề tài do chính mình đang hướng dẫn!");
        });
    }

    @Test
    @DisplayName("TC_LOCK_INSERT: Trigger trg_check_council_member_rules should block adding member after grading started")
    @Transactional
    void testCouncilLockdown_InsertBlockedAfterGradingStarted() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User chair = userRepository.findByUserCode("TBM001").orElseThrow();

        // 1. Chấm điểm khởi đầu để kích hoạt cờ fn_is_grading_started
        Evaluation initialEval = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(chair)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_CHAIR)
                .criteria1Score(new BigDecimal("8.00"))
                .criteria2Score(new BigDecimal("8.00"))
                .criteria3Score(new BigDecimal("8.00"))
                .totalScore(new BigDecimal("8.00"))
                .build();
        evaluationRepository.saveAndFlush(initialEval);

        // 2. Cố tình thêm giảng viên mới vào hội đồng sau khi đã có điểm
        User newLecturer = userRepository.findByUserCode("TK001").orElseThrow();
        CouncilMember newMember = CouncilMember.builder()
                .council(councilTopic.getCouncil())
                .lecturer(newLecturer)
                .position(CouncilMember.CouncilPosition.MEMBER)
                .build();

        assertThatThrownBy(() -> {
            councilMemberRepository.saveAndFlush(newMember);
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Hội đồng đã bắt đầu chấm điểm, không được phép thêm thành viên mới!");
        });
    }

    @Test
    @DisplayName("TC_LOCK_UPDATE: Trigger trg_prevent_council_member_update_after_grading should block updating member after grading started")
    @Transactional
    void testCouncilLockdown_UpdateBlockedAfterGradingStarted() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User chair = userRepository.findByUserCode("TBM001").orElseThrow();

        // 1. Chấm điểm khởi đầu
        Evaluation initialEval = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(chair)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_CHAIR)
                .criteria1Score(new BigDecimal("8.00"))
                .criteria2Score(new BigDecimal("8.00"))
                .criteria3Score(new BigDecimal("8.00"))
                .totalScore(new BigDecimal("8.00"))
                .build();
        evaluationRepository.saveAndFlush(initialEval);

        // 2. Cố tình sửa đổi vai trò của thành viên trong hội đồng
        CouncilMember member = councilMemberRepository.findByCouncilId(councilTopic.getCouncil().getId())
                .stream()
                .filter(cm -> cm.getLecturer().getUserCode().equals("GV003"))
                .findFirst().orElseThrow();
        member.setPosition(CouncilMember.CouncilPosition.REVIEWER);

        assertThatThrownBy(() -> {
            councilMemberRepository.saveAndFlush(member);
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Hội đồng đã bắt đầu chấm điểm, không được phép chỉnh sửa thông tin thành viên!");
        });
    }

    @Test
    @DisplayName("TC_LOCK_DELETE: Trigger trg_prevent_council_member_delete_after_grading should block deleting member after grading started")
    @Transactional
    void testCouncilLockdown_DeleteBlockedAfterGradingStarted() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User chair = userRepository.findByUserCode("TBM001").orElseThrow();

        // 1. Chấm điểm khởi đầu
        Evaluation initialEval = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(chair)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_CHAIR)
                .criteria1Score(new BigDecimal("8.00"))
                .criteria2Score(new BigDecimal("8.00"))
                .criteria3Score(new BigDecimal("8.00"))
                .totalScore(new BigDecimal("8.00"))
                .build();
        evaluationRepository.saveAndFlush(initialEval);

        // 2. Cố tình xóa thành viên khỏi hội đồng
        CouncilMember member = councilMemberRepository.findByCouncilId(councilTopic.getCouncil().getId())
                .stream()
                .filter(cm -> cm.getLecturer().getUserCode().equals("GV003"))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> {
            councilMemberRepository.delete(member);
            councilMemberRepository.flush();
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Hội đồng đã bắt đầu chấm điểm, không được phép xóa thành viên hội đồng!");
        });
    }
}
