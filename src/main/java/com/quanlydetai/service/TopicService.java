package com.quanlydetai.service;

import com.quanlydetai.aspect.AuditAction;
import com.quanlydetai.entity.Department;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.Topic;
import com.quanlydetai.entity.TopicSupervisor;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.DepartmentRepository;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.repository.TopicRepository;
import com.quanlydetai.repository.TopicSupervisorRepository;
import com.quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    public List<Topic> getTopicsByPeriod(Long periodId) {
        return topicRepository.findByPeriodId(periodId);
    }

    public List<Topic> getAvailableTopicsForRegistration(Long periodId) {
        return topicRepository.findAvailableTopicsForRegistration(periodId);
    }

    public Topic getTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài ID: " + id));
    }

    @Transactional
    @AuditAction(action = "PROPOSE_TOPIC", entityName = "Topic")
    public Topic proposeTopic(com.quanlydetai.dto.TopicCreateDto dto, User lecturer) {
        Topic topic = Topic.builder()
                .topicCode(dto.getTopicCode())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .requirements(dto.getRequirements())
                .build();
        return proposeTopic(topic, dto.getDepartmentId(), dto.getPeriodId(), lecturer, dto.getCoSupervisorId());
    }

    @Transactional
    @AuditAction(action = "PROPOSE_TOPIC", entityName = "Topic")
    public Topic proposeTopic(Topic topic, Long departmentId, Long periodId, User lecturer, Long coSupervisorId) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bộ môn"));
        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt"));

        // RÀNG BUỘC GIAI ĐOẠN 1: GV chỉ được đề xuất đề tài trong cửa sổ thời gian GV
        if (!period.isTopicSubmissionOpen()) {
            throw new IllegalStateException(
                    "Giai đoạn GV đề xuất đề tài chưa mở hoặc đã kết thúc. Vui lòng kiểm tra lịch đợt đăng ký.");
        }

        validateSupervisorsForCreation(lecturer != null ? lecturer.getId() : null, coSupervisorId);

        topic.setDepartment(dept);
        topic.setPeriod(period);
        topic.setCreatedByLecturer(lecturer);
        topic.setStatus(Topic.TopicStatus.PENDING_APPROVAL);

        Topic saved = topicRepository.save(topic);

        // Thêm GVHD chính
        TopicSupervisor primarySupervisor = TopicSupervisor.builder()
                .topic(saved)
                .lecturer(lecturer)
                .isPrimary(true)
                .build();
        supervisorRepository.save(primarySupervisor);

        // Thêm GVHD phụ nếu có (tối đa 2 GVHD)
        if (coSupervisorId != null && !coSupervisorId.equals(lecturer.getId())) {
            User coLecturer = userRepository.findById(coSupervisorId).orElse(null);
            if (coLecturer != null) {
                TopicSupervisor secondarySupervisor = TopicSupervisor.builder()
                        .topic(saved)
                        .lecturer(coLecturer)
                        .isPrimary(false)
                        .build();
                supervisorRepository.save(secondarySupervisor);
            }
        }

        return saved;
    }

    @Transactional
    @AuditAction(action = "APPROVE_TOPIC", entityName = "Topic")
    public void approveTopic(Long topicId, User approver) {
        Topic topic = getTopicById(topicId);
        topic.setStatus(Topic.TopicStatus.APPROVED);
        topic.setApprovedBy(approver);
        topic.setRejectReason(null);
        topicRepository.save(topic);
    }

    @Transactional
    @AuditAction(action = "REJECT_TOPIC", entityName = "Topic")
    public void rejectTopic(Long topicId, String reason, User approver) {
        Topic topic = getTopicById(topicId);
        topic.setStatus(Topic.TopicStatus.REJECTED);
        topic.setApprovedBy(approver);
        topic.setRejectReason(reason);
        topicRepository.save(topic);
    }

    public void validateSupervisorsForCreation(Long primaryLecturerId, Long coSupervisorId) {
        if (primaryLecturerId == null) {
            throw new IllegalArgumentException("Quy chế: Đề tài bắt buộc phải có Giảng viên hướng dẫn chính!");
        }
        if (coSupervisorId != null && coSupervisorId.equals(primaryLecturerId)) {
            throw new IllegalArgumentException("Quy chế: Giảng viên đồng hướng dẫn không được trùng với Giảng viên hướng dẫn chính!");
        }
    }
}
