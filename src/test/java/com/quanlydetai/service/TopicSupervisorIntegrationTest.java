package com.quanlydetai.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
import com.quanlydetai.util.SqlErrorUtils;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TopicSupervisorIntegrationTest {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TopicSupervisorRepository supervisorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegistrationPeriodRepository periodRepository;

    @Autowired
    private TopicService topicService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("TC-SUP-01: Đề tài có 2 GVHD hiển thị đúng tên chính và đồng hướng dẫn")
    @Transactional
    void testDualSupervisorsDisplay() {
        Topic topic1 = topicRepository.findById(1L).orElseThrow();
        assertThat(topic1.hasCoSupervisor()).isTrue();
        assertThat(topic1.getPrimarySupervisor()).isNotNull();
        assertThat(topic1.getPrimarySupervisor().getUserCode()).isEqualTo("GV001");
        assertThat(topic1.getCoSupervisor()).isNotNull();
        assertThat(topic1.getCoSupervisor().getUserCode()).isEqualTo("GV002");
        assertThat(topic1.getSupervisorsDisplayString())
                .isEqualTo(topic1.getPrimarySupervisor().getFullName() + " (Chính), " + topic1.getCoSupervisor().getFullName() + " (Đồng HD)");
    }

    @Test
    @DisplayName("TC-SUP-02: Đề tài có 1 GVHD hiển thị gọn gàng không tag thừa")
    @Transactional
    void testSingleSupervisorDisplay() {
        Topic topic2 = topicRepository.findById(2L).orElseThrow();
        assertThat(topic2.hasCoSupervisor()).isFalse();
        assertThat(topic2.getPrimarySupervisor()).isNotNull();
        assertThat(topic2.getCoSupervisor()).isNull();
        assertThat(topic2.getSupervisorsDisplayString()).isEqualTo(topic2.getPrimarySupervisor().getFullName());
    }

    @Test
    @DisplayName("TC-SUP-03: Log warning khi dữ liệu supervisors bất thường (không có primary)")
    void testLogWarningWhenNoPrimarySupervisor() {
        Logger topicLogger = (Logger) LoggerFactory.getLogger(Topic.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        topicLogger.addAppender(listAppender);

        try {
            User creator = User.builder().id(99L).fullName("TS. Fallback").build();
            User otherLecturer = User.builder().id(100L).fullName("TS. Other").build();
            Topic testTopic = Topic.builder()
                    .id(9999L)
                    .title("Test Topic Data Anomaly")
                    .createdByLecturer(creator)
                    .supervisors(new ArrayList<>())
                    .build();

            TopicSupervisor tsWithoutPrimary = TopicSupervisor.builder()
                    .topic(testTopic)
                    .lecturer(otherLecturer)
                    .isPrimary(false)
                    .build();
            testTopic.getSupervisors().add(tsWithoutPrimary);

            User result = testTopic.getPrimarySupervisor();
            assertThat(result).isEqualTo(creator);

            assertThat(listAppender.list).anyMatch(event ->
                    event.getLevel() == Level.WARN &&
                    event.getFormattedMessage().contains("có 1 supervisors nhưng không có ai mang is_primary = true")
            );
        } finally {
            topicLogger.detachAppender(listAppender);
        }
    }

    @Test
    @DisplayName("TC-TRIG-01: Trigger MySQL chặn thêm giảng viên thứ 3 vào đề tài (tối đa 2 GV)")
    @Transactional
    void testTriggerBlocksThirdSupervisor() {
        Topic topic1 = topicRepository.findById(1L).orElseThrow();
        User gv003 = userRepository.findByUserCode("GV003").orElseThrow();

        TopicSupervisor thirdSupervisor = TopicSupervisor.builder()
                .topic(topic1)
                .lecturer(gv003)
                .isPrimary(false)
                .build();

        assertThatThrownBy(() -> {
            supervisorRepository.saveAndFlush(thirdSupervisor);
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Quy chế: Mỗi đề tài chỉ được hướng dẫn bởi tối đa 2 giảng viên!");
        });
    }

    @Test
    @DisplayName("TC-TRIG-02: Trigger MySQL chặn thêm GVHD chính thứ 2 vào đề tài")
    @Transactional
    void testTriggerBlocksSecondPrimarySupervisor() {
        // Tạo 1 đề tài mới có 1 GVHD chính
        User gv001 = userRepository.findByUserCode("GV001").orElseThrow();
        User gv002 = userRepository.findByUserCode("GV002").orElseThrow();
        Department dept = departmentRepository.findById(1L).orElseThrow();
        RegistrationPeriod period = periodRepository.findById(1L).orElseThrow();

        Topic newTopic = Topic.builder()
                .topicCode("DT_TEST_TRIG_02")
                .title("Test Second Primary")
                .description("Desc")
                .department(dept)
                .period(period)
                .createdByLecturer(gv001)
                .status(Topic.TopicStatus.APPROVED)
                .build();
        newTopic = topicRepository.saveAndFlush(newTopic);

        TopicSupervisor primary1 = TopicSupervisor.builder()
                .topic(newTopic)
                .lecturer(gv001)
                .isPrimary(true)
                .build();
        supervisorRepository.saveAndFlush(primary1);

        TopicSupervisor primary2 = TopicSupervisor.builder()
                .topic(newTopic)
                .lecturer(gv002)
                .isPrimary(true)
                .build();

        assertThatThrownBy(() -> {
            supervisorRepository.saveAndFlush(primary2);
        }).satisfies(throwable -> {
            String friendlyMsg = SqlErrorUtils.extractFriendlyMessage(throwable);
            assertThat(friendlyMsg).isEqualTo("Quy chế: Mỗi đề tài chỉ được có duy nhất 1 giảng viên hướng dẫn chính!");
        });
    }

    @Test
    @DisplayName("TC-REGRESS-DELETE: Cascade delete Topic có 2 supervisors thành công, không bị lỗi trigger DELETE")
    @Transactional
    void testCascadeDeleteTopicWithTwoSupervisors() {
        User gv001 = userRepository.findByUserCode("GV001").orElseThrow();
        User gv002 = userRepository.findByUserCode("GV002").orElseThrow();
        Department dept = departmentRepository.findById(1L).orElseThrow();
        RegistrationPeriod period = periodRepository.findById(1L).orElseThrow();

        Topic testTopic = Topic.builder()
                .topicCode("DT_TEST_REGRESS_DEL")
                .title("Test Cascade Delete")
                .description("Desc")
                .department(dept)
                .period(period)
                .createdByLecturer(gv001)
                .status(Topic.TopicStatus.APPROVED)
                .build();
        testTopic = topicRepository.saveAndFlush(testTopic);

        TopicSupervisor primary = TopicSupervisor.builder()
                .topic(testTopic)
                .lecturer(gv001)
                .isPrimary(true)
                .build();
        TopicSupervisor secondary = TopicSupervisor.builder()
                .topic(testTopic)
                .lecturer(gv002)
                .isPrimary(false)
                .build();
        testTopic.getSupervisors().add(primary);
        testTopic.getSupervisors().add(secondary);
        supervisorRepository.save(primary);
        supervisorRepository.save(secondary);
        topicRepository.saveAndFlush(testTopic);

        entityManager.flush();
        entityManager.clear();

        // Verify inserted
        List<TopicSupervisor> supervisorsBefore = supervisorRepository.findByTopicId(testTopic.getId());
        assertThat(supervisorsBefore).hasSize(2);

        // Delete parent Topic -> should cascade delete both supervisors without trigger 45000 failure
        Topic toDelete = topicRepository.findById(testTopic.getId()).orElseThrow();
        topicRepository.delete(toDelete);
        topicRepository.flush();

        entityManager.clear();

        List<TopicSupervisor> supervisorsAfter = supervisorRepository.findByTopicId(testTopic.getId());
        assertThat(supervisorsAfter).isEmpty();
        assertThat(topicRepository.findById(testTopic.getId())).isEmpty();
    }

    @Test
    @DisplayName("TC-SVC-VALIDATE: TopicService validateSupervisorsForCreation xử lý đúng các ràng buộc")
    void testServiceValidateSupervisorsForCreation() {
        // Primary is null -> exception
        assertThatThrownBy(() -> topicService.validateSupervisorsForCreation(null, 4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Đề tài bắt buộc phải có Giảng viên hướng dẫn chính");

        // Primary == CoSupervisor -> exception
        assertThatThrownBy(() -> topicService.validateSupervisorsForCreation(3L, 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Giảng viên đồng hướng dẫn không được trùng");

        // Valid cases -> no exception
        topicService.validateSupervisorsForCreation(3L, null);
        topicService.validateSupervisorsForCreation(3L, 4L);
    }
}
