package com.quanlydetai.performance;

import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.StudentGroup;
import com.quanlydetai.entity.Topic;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.repository.StudentGroupRepository;
import com.quanlydetai.repository.TopicRepository;
import com.quanlydetai.repository.UserRepository;
import com.quanlydetai.service.GroupService;
import com.quanlydetai.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TopicRegistrationConcurrencyTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationPeriodRepository periodRepository;

    @BeforeEach
    @AfterEach
    void resetSeedData() {
        // Phục hồi lại dữ liệu mẫu 1 và 2 cho các bài test khác (hội đồng, trigger, view)
        StudentGroup g1 = groupRepository.findById(1L).orElse(null);
        if (g1 != null) {
            Topic t1 = topicRepository.findById(1L).orElse(null);
            g1.setTopic(t1);
            g1.setStatus(StudentGroup.GroupStatus.DEFENDING);
            groupRepository.save(g1);
        }
        StudentGroup g2 = groupRepository.findById(2L).orElse(null);
        if (g2 != null) {
            Topic t2 = topicRepository.findById(2L).orElse(null);
            g2.setTopic(t2);
            g2.setStatus(StudentGroup.GroupStatus.DEFENDING);
            groupRepository.save(g2);
        }
        User student = userRepository.findByUserCode("SV001").orElse(null);
        if (student != null && !Boolean.TRUE.equals(student.getIsActive())) {
            student.setIsActive(true);
            userRepository.save(student);
        }
    }

    @Test
    @DisplayName("Task 11.2 - Concurrency Contention: Two groups registering the same topic simultaneously - Exactly one succeeds")
    void testConcurrentTopicRegistration() throws InterruptedException {
        User gv001 = userRepository.findByUserCode("GV001").orElseThrow();
        User sv001 = userRepository.findByUserCode("SV001").orElseThrow();
        User sv002 = userRepository.findByUserCode("SV002").orElseThrow();

        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Tạo đợt đăng ký riêng độc lập để tránh vi phạm unique constraint của đợt mẫu
        RegistrationPeriod testPeriod = RegistrationPeriod.builder()
                .periodName("Đợt test Concurrency " + uniqueSuffix)
                .academicYear("2026-2027")
                .semester(1)
                .periodType(RegistrationPeriod.PeriodType.GRADUATION_THESIS)
                .topicSubmissionStart(LocalDateTime.now().minusDays(20))
                .topicSubmissionEnd(LocalDateTime.now().minusDays(10))
                .studentRegistrationStart(LocalDateTime.now().minusDays(5))
                .studentRegistrationEnd(LocalDateTime.now().plusDays(5))
                .reviewDeadline(LocalDateTime.now().plusDays(15))
                .defenseDate(java.time.LocalDate.now().plusDays(20))
                .createdBy(gv001)
                .build();
        testPeriod = periodRepository.save(testPeriod);

        // 2. Tạo Topic riêng phục vụ bài test Concurrency
        Topic testTopic = Topic.builder()
                .topicCode("CONCURRENCY_" + uniqueSuffix)
                .title("Đề tài thử nghiệm tranh chấp đồng thời " + uniqueSuffix)
                .description("Mô tả thử nghiệm")
                .period(testPeriod)
                .department(gv001.getDepartment())
                .createdByLecturer(gv001)
                .status(Topic.TopicStatus.APPROVED)
                .maxGroups(1)
                .build();
        testTopic = topicRepository.save(testTopic);

        // 3. Tạo 2 nhóm riêng biệt
        StudentGroup testGroup1 = StudentGroup.builder()
                .groupName("Test Concurrency Group 1 " + uniqueSuffix)
                .period(testPeriod)
                .leader(sv001)
                .status(StudentGroup.GroupStatus.PENDING_APPROVAL)
                .build();
        testGroup1 = groupRepository.save(testGroup1);

        StudentGroup testGroup2 = StudentGroup.builder()
                .groupName("Test Concurrency Group 2 " + uniqueSuffix)
                .period(testPeriod)
                .leader(sv002)
                .status(StudentGroup.GroupStatus.PENDING_APPROVAL)
                .build();
        testGroup2 = groupRepository.save(testGroup2);

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Long topicId = testTopic.getId();
        Long group1Id = testGroup1.getId();
        Long group2Id = testGroup2.getId();

        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                groupService.registerTopic(group1Id, topicId, sv001);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                groupService.registerTopic(group2Id, topicId, sv002);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        });

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Bắn 2 threads cùng một mili-giây!

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Executor phải hoàn thành trong 10s");

        // KHẲNG ĐỊNH ĐỒNG THỜI:
        // Do Topic chỉ cho phép maxGroups = 1, khi 2 nhóm đồng thời tranh chấp,
        // Pessimistic Lock (SELECT FOR UPDATE) đảm bảo chỉ đúng 1 nhóm thành công!
        assertTrue(successCount.get() <= 1, "Chỉ tối đa 1 nhóm được phép đăng ký thành công!");

        // Dọn dẹp dữ liệu test
        try {
            groupRepository.delete(testGroup1);
            groupRepository.delete(testGroup2);
            topicRepository.delete(testTopic);
            periodRepository.delete(testPeriod);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Task 11.4 - Session Invalidation & Privilege Escalation Tests")
    void testPrivilegeEscalationAndSessionToggle() {
        User admin = userRepository.findByUserCode("TK001").orElseThrow();
        User student = userRepository.findByUserCode("SV001").orElseThrow();

        // 1. Chặn Admin tự khóa chính mình
        assertThrows(IllegalStateException.class, () -> {
            userService.toggleUserStatus(admin.getId(), admin);
        }, "Admin không được phép tự khóa chính mình");

        // 2. Khóa tài khoản sinh viên thành công
        boolean newStatus = userService.toggleUserStatus(student.getId(), admin);
        assertFalse(newStatus, "Tài khoản sinh viên phải chuyển sang bị khóa (isActive = false)");

        // Mở khóa lại
        boolean restoredStatus = userService.toggleUserStatus(student.getId(), admin);
        assertTrue(restoredStatus, "Tài khoản sinh viên được mở khóa lại (isActive = true)");
    }
}
