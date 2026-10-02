package com.quanlydetai.service;

import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CouncilProcedureIntegrationTest {

    @Autowired
    private CouncilService councilService;

    @Autowired
    private CouncilTopicRepository councilTopicRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Test
    @DisplayName("recalculateFinalScore should invoke sp_calculate_final_council_score stored procedure")
    @Transactional
    void testRecalculateFinalScoreViaStoredProcedure() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User gv1 = userRepository.findByUserCode("TK001").orElseThrow();
        User gv2 = userRepository.findByUserCode("GV003").orElseThrow();

        Evaluation eval1 = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(gv1)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_CHAIR)
                .criteria1Score(new BigDecimal("8.00"))
                .criteria2Score(new BigDecimal("8.00"))
                .criteria3Score(new BigDecimal("8.00"))
                .totalScore(new BigDecimal("8.00"))
                .build();

        Evaluation eval2 = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(gv2)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_MEMBER)
                .criteria1Score(new BigDecimal("9.00"))
                .criteria2Score(new BigDecimal("9.00"))
                .criteria3Score(new BigDecimal("9.00"))
                .totalScore(new BigDecimal("9.00"))
                .build();

        evaluationRepository.saveAndFlush(eval1);
        evaluationRepository.saveAndFlush(eval2);

        // Execute procedure via councilService
        councilService.recalculateFinalScore(councilTopic);

        // Expected average = (8.00 + 9.00) / 2 = 8.50
        BigDecimal expectedAvg = new BigDecimal("8.50");
        assertThat(councilTopic.getFinalCouncilScore()).isEqualByComparingTo(expectedAvg);

        // Reload group from database to verify procedure updated student_groups directly
        StudentGroup group = groupRepository.findById(councilTopic.getGroup().getId()).orElseThrow();
        assertThat(group.getFinalScore()).isEqualByComparingTo(expectedAvg);
    }

    @Test
    @DisplayName("TC_PUB_PARTIAL_GRADING: publishResults should fail when not all council members have graded")
    @Transactional
    void testPublishResults_PartialGradingFails() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User chair = userRepository.findByUserCode("TBM001").orElseThrow();

        // Hội đồng có 3 thành viên, chỉ 1 người chấm
        Evaluation eval = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(chair)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_CHAIR)
                .criteria1Score(new BigDecimal("8.00"))
                .criteria2Score(new BigDecimal("8.00"))
                .criteria3Score(new BigDecimal("8.00"))
                .totalScore(new BigDecimal("8.00"))
                .build();
        evaluationRepository.saveAndFlush(eval);

        councilService.recalculateFinalScore(councilTopic);

        assertThatThrownBy(() -> {
            councilService.publishResults(councilTopic.getId());
        }).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("chưa hoàn tất chấm điểm");
    }

    @Test
    @DisplayName("TC_PUB_ALL_GRADED: publishResults should succeed when all council members graded, setting published_at and COMPLETED status")
    @Transactional
    void testPublishResults_AllGradedSucceeds() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        Council council = councilTopic.getCouncil();

        // Chuẩn bị 3 thành viên không trùng GVHD (GV001, GV002 đang HD đề tài 1)
        // Thay GV002 trong hội đồng bằng TK001 TRƯỚC KHI chấm điểm
        CouncilMember gv002Member = councilMemberRepository.findByCouncilId(council.getId()).stream()
                .filter(cm -> cm.getLecturer().getUserCode().equals("GV002"))
                .findFirst().orElseThrow();
        councilMemberRepository.delete(gv002Member);
        councilMemberRepository.flush();

        User tk001 = userRepository.findByUserCode("TK001").orElseThrow();
        CouncilMember tk001Member = CouncilMember.builder()
                .council(council)
                .lecturer(tk001)
                .position(CouncilMember.CouncilPosition.MEMBER)
                .build();
        councilMemberRepository.saveAndFlush(tk001Member);

        // Giờ hội đồng 1 có 3 thành viên: TBM001 (CHAIR), GV003 (SECRETARY), TK001 (MEMBER)
        User tbm001 = userRepository.findByUserCode("TBM001").orElseThrow();
        User gv003 = userRepository.findByUserCode("GV003").orElseThrow();

        Evaluation eval1 = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(tbm001)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_CHAIR)
                .criteria1Score(new BigDecimal("8.00"))
                .criteria2Score(new BigDecimal("8.00"))
                .criteria3Score(new BigDecimal("8.00"))
                .totalScore(new BigDecimal("8.00"))
                .build();

        Evaluation eval2 = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(gv003)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_MEMBER)
                .criteria1Score(new BigDecimal("9.00"))
                .criteria2Score(new BigDecimal("9.00"))
                .criteria3Score(new BigDecimal("9.00"))
                .totalScore(new BigDecimal("9.00"))
                .build();

        Evaluation eval3 = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(tk001)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_MEMBER)
                .criteria1Score(new BigDecimal("10.00"))
                .criteria2Score(new BigDecimal("10.00"))
                .criteria3Score(new BigDecimal("10.00"))
                .totalScore(new BigDecimal("10.00"))
                .build();

        evaluationRepository.saveAndFlush(eval1);
        evaluationRepository.saveAndFlush(eval2);
        evaluationRepository.saveAndFlush(eval3);

        // Tính điểm tổng kết = (8 + 9 + 10) / 3 = 9.00
        councilService.recalculateFinalScore(councilTopic);

        // Công bố kết quả
        councilService.publishResults(councilTopic.getId());

        // Kiểm tra kết quả sau khi công bố
        CouncilTopic publishedTopic = councilTopicRepository.findById(councilTopic.getId()).orElseThrow();
        assertThat(publishedTopic.getIsPublished()).isTrue();
        assertThat(publishedTopic.getPublishedAt()).isNotNull();
        assertThat(publishedTopic.getFinalCouncilScore()).isEqualByComparingTo(new BigDecimal("9.00"));

        StudentGroup group = groupRepository.findById(publishedTopic.getGroup().getId()).orElseThrow();
        assertThat(group.getStatus()).isEqualTo(StudentGroup.GroupStatus.COMPLETED);
        assertThat(group.getFinalScore()).isEqualByComparingTo(new BigDecimal("9.00"));

        Topic topic = topicRepository.findById(group.getTopic().getId()).orElseThrow();
        assertThat(topic.getStatus()).isEqualTo(Topic.TopicStatus.COMPLETED);
    }

    @Test
    @DisplayName("TC_RECALC_ON_DELETE_TO_ZERO: Trigger AFTER evaluations should auto-recalculate score and reset to NULL on delete to zero")
    @Transactional
    void testAutoRecalculateOnInsertAndUpdateAndDeleteToZero() {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User gv1 = userRepository.findByUserCode("TK001").orElseThrow();

        // 1. Insert 1 evaluation -> trigger trg_after_eval_insert_recalc tự động tính score = 8.00 ngay trong DB
        Evaluation eval1 = Evaluation.builder()
                .councilTopic(councilTopic)
                .evaluatorLecturer(gv1)
                .evaluationType(Evaluation.EvaluationType.COUNCIL_CHAIR)
                .criteria1Score(new BigDecimal("8.00"))
                .criteria2Score(new BigDecimal("8.00"))
                .criteria3Score(new BigDecimal("8.00"))
                .totalScore(new BigDecimal("8.00"))
                .build();
        evaluationRepository.saveAndFlush(eval1);
        entityManager.clear();

        CouncilTopic reloadedCt1 = councilTopicRepository.findById(1L).orElseThrow();
        assertThat(reloadedCt1.getFinalCouncilScore()).isEqualByComparingTo("8.00");
        StudentGroup group1 = groupRepository.findById(reloadedCt1.getGroup().getId()).orElseThrow();
        assertThat(group1.getFinalScore()).isEqualByComparingTo("8.00");

        // 2. Delete all evaluations -> trigger trg_after_eval_delete_recalc tự động reset điểm về NULL (không để orphan score)
        Evaluation evalToDelete = evaluationRepository.findById(eval1.getId()).orElseThrow();
        evaluationRepository.delete(evalToDelete);
        evaluationRepository.flush();
        entityManager.clear();

        CouncilTopic reloadedCt2 = councilTopicRepository.findById(1L).orElseThrow();
        assertThat(reloadedCt2.getFinalCouncilScore()).isNull();
        StudentGroup group2 = groupRepository.findById(reloadedCt2.getGroup().getId()).orElseThrow();
        assertThat(group2.getFinalScore()).isNull();
    }
}
