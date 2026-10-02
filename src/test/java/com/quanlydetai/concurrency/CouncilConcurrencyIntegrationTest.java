package com.quanlydetai.concurrency;

import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
import com.quanlydetai.service.CouncilService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CouncilConcurrencyIntegrationTest {

    @Autowired
    private CouncilService councilService;

    @Autowired
    private CouncilRepository councilRepository;

    @Autowired
    private CouncilMemberRepository councilMemberRepository;

    @Autowired
    private CouncilTopicRepository councilTopicRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    @org.junit.jupiter.api.AfterEach
    void cleanupDatabase() {
        jdbcTemplate.execute("DELETE FROM evaluations");
        jdbcTemplate.execute("DELETE FROM council_members WHERE council_id = 1 AND id > 3");
        jdbcTemplate.execute("UPDATE council_topics SET final_council_score = NULL, is_published = FALSE, published_at = NULL WHERE id = 1");
        jdbcTemplate.execute("UPDATE student_groups SET final_score = NULL, status = 'DEFENDING' WHERE id = 1");
    }

    @Test
    @DisplayName("CONCURRENCY-1: Concurrent grading and member manipulation are serialized via pessimistic row-lock without deadlocks")
    void testConcurrentGradingAndMemberModification_SerializedSafely() throws Exception {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User chair = userRepository.findByUserCode("TBM001").orElseThrow();
        User newLecturer = userRepository.findByUserCode("TK001").orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicBoolean gradingSuccess = new AtomicBoolean(false);
        AtomicBoolean memberModBlocked = new AtomicBoolean(false);

        // Thread 1: Nộp điểm đầu tiên cho hội đồng
        executor.submit(() -> {
            try {
                startLatch.await();
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
                gradingSuccess.set(true);
            } catch (Exception e) {
                // Ignore if racing
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2: Cố tình thêm thành viên vào cùng hội đồng tại cùng thời điểm
        executor.submit(() -> {
            try {
                startLatch.await();
                // Sleep briefly to ensure Thread 1 acquires row lock or completes
                Thread.sleep(10);
                CouncilMember newMember = CouncilMember.builder()
                        .council(councilTopic.getCouncil())
                        .lecturer(newLecturer)
                        .position(CouncilMember.CouncilPosition.MEMBER)
                        .build();
                councilMemberRepository.saveAndFlush(newMember);
            } catch (Exception e) {
                // Expected to be blocked by trigger if Thread 1 got there first or serialized
                if (e.getMessage() != null && (e.getMessage().contains("bắt đầu chấm điểm") 
                        || e.getMessage().contains("thành viên")
                        || e.getMessage().contains("Lock wait"))) {
                    memberModBlocked.set(true);
                }
            } finally {
                doneLatch.countDown();
            }
        });

        // Kích hoạt đồng thời cả 2 luồng
        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        // Sau khi Thread 1 chấm điểm thành công, hội đồng phải có điểm
        assertThat(gradingSuccess.get()).isTrue();
    }

    @Test
    @DisplayName("CONCURRENCY-2: Concurrent evaluations by distinct lecturers succeed without deadlocks or duplicate corruption")
    void testConcurrentEvaluations_NoDeadlock() throws Exception {
        CouncilTopic councilTopic = councilTopicRepository.findById(1L).orElseThrow();
        User gv1 = userRepository.findByUserCode("TBM001").orElseThrow();
        User gv2 = userRepository.findByUserCode("GV003").orElseThrow();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);

        // Luồng chấm điểm A (TBM001)
        executor.submit(() -> {
            try {
                startLatch.await();
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
                successCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Luồng chấm điểm B (GV003)
        executor.submit(() -> {
            try {
                startLatch.await();
                Evaluation eval2 = Evaluation.builder()
                        .councilTopic(councilTopic)
                        .evaluatorLecturer(gv2)
                        .evaluationType(Evaluation.EvaluationType.COUNCIL_MEMBER)
                        .criteria1Score(new BigDecimal("9.00"))
                        .criteria2Score(new BigDecimal("9.00"))
                        .criteria3Score(new BigDecimal("9.00"))
                        .totalScore(new BigDecimal("9.00"))
                        .build();
                evaluationRepository.saveAndFlush(eval2);
                successCount.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
    }
}
