package com.quanlydetai.service;

import com.quanlydetai.entity.CouncilTopic;
import com.quanlydetai.entity.Evaluation;
import com.quanlydetai.entity.StudentGroup;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.CouncilTopicRepository;
import com.quanlydetai.repository.EvaluationRepository;
import com.quanlydetai.repository.StudentGroupRepository;
import com.quanlydetai.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouncilProcedureIntegrationTest {

    @Autowired
    private CouncilService councilService;

    @Autowired
    private CouncilTopicRepository councilTopicRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private UserRepository userRepository;

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
}
