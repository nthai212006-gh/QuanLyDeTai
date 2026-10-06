package com.quanlydetai.service;

import com.quanlydetai.aspect.AuditAction;
import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouncilService {

    @PersistenceContext
    private EntityManager entityManager;

    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final EvaluationRepository evaluationRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final StudentGroupRepository groupRepository;
    private final TopicRepository topicRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;

    public List<Council> getCouncilsByPeriod(Long periodId) {
        return councilRepository.findByPeriodId(periodId);
    }

    public List<Council> getAllCouncils() {
        return councilRepository.findAllByOrderByCreatedAtDesc();
    }

    public Council getCouncilById(Long id) {
        return councilRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hội đồng ID: " + id));
    }

    public CouncilTopic getCouncilTopicById(Long id) {
        return councilTopicRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài trong hội đồng"));
    }

    @Transactional
    @AuditAction(action = "CREATE_COUNCIL", entityName = "Council")
    public Council createCouncil(String councilCode, String councilName, Long periodId, LocalDateTime defenseDate, String location) {
        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt"));

        Council council = Council.builder()
                .councilCode(councilCode)
                .councilName(councilName)
                .period(period)
                .defenseDate(defenseDate)
                .location(location)
                .status(Council.CouncilStatus.CREATED)
                .build();

        return councilRepository.save(council);
    }

    @Transactional
    @AuditAction(action = "ADD_COUNCIL_MEMBER", entityName = "CouncilMember")
    public void addCouncilMember(Long councilId, Long lecturerId, CouncilMember.CouncilPosition position) {
        Council council = getCouncilById(councilId);
        long currentMembersCount = memberRepository.countByCouncilId(councilId);
        if (currentMembersCount >= 5) {
            throw new IllegalStateException("Quy định: Mỗi hội đồng chỉ gồm từ 03 đến tối đa 05 giảng viên!");
        }

        // RÀNG BUỘC: Mỗi hội đồng chỉ có duy nhất 1 CHỦ TỊCH và 1 THƯ KÝ
        if ((position == CouncilMember.CouncilPosition.CHAIR || position == CouncilMember.CouncilPosition.SECRETARY)
                && memberRepository.existsByCouncilIdAndPosition(councilId, position)) {
            String posName = position == CouncilMember.CouncilPosition.CHAIR ? "CHỦ TỊCH" : "THU KY";
            throw new IllegalStateException("Hội đồng đã có " + posName + ". Mỗi hội đồng chỉ có duy nhất 1 " + posName + "!");
        }

        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên"));

        // RÀNG BUỘC: Giảng viên không được tham gia hội đồng nếu đang hướng dẫn đề tài đã được gán vào hội đồng
        List<CouncilTopic> assignedTopics = councilTopicRepository.findByCouncilId(councilId);
        for (CouncilTopic ct : assignedTopics) {
            Topic topic = ct.getGroup() != null ? ct.getGroup().getTopic() : null;
            if (topic != null) {
                boolean isSupervisor = supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), lecturerId)
                        || (topic.getCreatedByLecturer() != null && topic.getCreatedByLecturer().getId().equals(lecturerId));
                if (isSupervisor) {
                    throw new IllegalStateException("Quy tắc nghiệp vụ: Giảng viên không được tham gia hội đồng chấm đề tài mà mình đang hướng dẫn ('" + topic.getTitle() + "')!");
                }
            }
        }

        CouncilMember member = CouncilMember.builder()
                .council(council)
                .lecturer(lecturer)
                .position(position)
                .build();

        memberRepository.save(member);
    }

    @Transactional
    @AuditAction(action = "REMOVE_COUNCIL_MEMBER", entityName = "CouncilMember")
    public void removeCouncilMember(Long councilId, Long lecturerId) {
        CouncilMember member = memberRepository.findByCouncilIdAndLecturerId(councilId, lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên này trong hội đồng!"));

        memberRepository.delete(member);
        memberRepository.flush();
    }

    /**
     * Kiểm tra một user có đang là CHỦ TỊCH của hội đồng đó không.
     * Dùng để cấp quyền publish kết quả cho CHAIR.
     */
    public boolean isChairOfCouncil(Long councilId, Long userId) {
        return memberRepository.existsByCouncilIdAndLecturerIdAndPosition(
                councilId, userId, CouncilMember.CouncilPosition.CHAIR);
    }


    @Transactional
    @AuditAction(action = "ASSIGN_TOPIC_TO_COUNCIL", entityName = "CouncilTopic")
    public CouncilTopic assignTopicToCouncil(Long councilId, Long groupId, Long reviewerId, Integer order) {
        Council council = getCouncilById(councilId);
        
        // RÀNG BUỘC 1: Hội đồng phải có ngày báo cáo hợp lệ
        if (council.getDefenseDate() == null) {
            throw new IllegalStateException("Hội đồng chưa được cấu hình ngày báo cáo bảo vệ!");
        }

        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm sinh viên!"));

        // RÀNG BUỘC 2: Nhóm sinh viên phải tồn tại đề tài đã được phê duyệt mới được xếp lịch
        if (group.getTopic() == null) {
            throw new IllegalStateException("Nhóm sinh viên này chưa đăng ký đề tài nào!");
        }
        if (group.getTopic().getStatus() != Topic.TopicStatus.APPROVED) {
            throw new IllegalStateException("Đề tài của nhóm chưa được duyệt (APPROVED), không thể xếp lịch bảo vệ!");
        }

        Topic topic = group.getTopic();

        // RÀNG BUỘC 3: Giảng viên hướng dẫn không được phân công làm cán bộ phản biện (GVPB)
        if (reviewerId != null) {
            boolean isReviewerSupervisor = supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), reviewerId)
                    || (topic.getCreatedByLecturer() != null && topic.getCreatedByLecturer().getId().equals(reviewerId));
            if (isReviewerSupervisor) {
                throw new IllegalStateException("Quy định: Giảng viên hướng dẫn không được phân công làm cán bộ phản biện (GVPB) cho chính đề tài mình hướng dẫn!");
            }
        }

        // RÀNG BUỘC 4: Không gán đề tài vào hội đồng nếu GVHD của đề tài đã là thành viên của hội đồng đó
        List<CouncilMember> councilMembers = memberRepository.findByCouncilId(councilId);
        for (CouncilMember cm : councilMembers) {
            Long memLecturerId = cm.getLecturer().getId();
            boolean isMemSupervisor = supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), memLecturerId)
                    || (topic.getCreatedByLecturer() != null && topic.getCreatedByLecturer().getId().equals(memLecturerId));
            if (isMemSupervisor) {
                throw new IllegalStateException("Quy định: Giảng viên " + cm.getLecturer().getFullName() +
                        " đang hướng dẫn đề tài này và đã là thành viên hội đồng. Một GV không được chấm đề tài mà mình đang hướng dẫn!");
            }
        }

        User reviewer = reviewerId != null ? userRepository.findById(reviewerId).orElse(null) : null;

        CouncilTopic councilTopic = CouncilTopic.builder()
                .council(council)
                .group(group)
                .reviewerLecturer(reviewer)
                .defenseOrder(order)
                .isPublished(false)
                .build();

        group.setStatus(StudentGroup.GroupStatus.DEFENDING);
        groupRepository.save(group);

        return councilTopicRepository.save(councilTopic);
    }

    @Transactional
    @AuditAction(action = "GRADE_TOPIC", entityName = "Evaluation")
    public Evaluation gradeTopic(Long councilTopicId, User evaluator, Evaluation.EvaluationType evalType,
                                BigDecimal c1, BigDecimal c2, BigDecimal c3, String comments) {
        CouncilTopic councilTopic = getCouncilTopicById(councilTopicId);
        Council council = councilTopic.getCouncil();

        // RÀNG BUỘC 3: Hội đồng phải đủ từ 3 đến 5 giảng viên mới được phép tiến hành chấm điểm
        long memberCount = memberRepository.countByCouncilId(council.getId());
        if (memberCount < 3) {
            throw new IllegalStateException("Hội đồng hiện mới có " + memberCount + " thành viên. Quy chế yêu cầu hội đồng phải có đủ ít nhất 3 giảng viên mới được chấm điểm!");
        }

        Topic topic = councilTopic.getGroup().getTopic();

        // QUY TẮC RÀNG BUỘC CỐT LÕI: GV KHÔNG ĐƯỢC CHẤM ĐỀ TÀI MÌNH ĐANG HƯỚNG DẪN
        if (topic != null) {
            boolean isSupervisor = supervisorRepository.existsByTopicIdAndLecturerId(topic.getId(), evaluator.getId());
            if (isSupervisor) {
                throw new IllegalStateException("Quy tắc nghiệp vụ: Giảng viên không được chấm đề tài mà mình đang hướng dẫn!");
            }
        }

        // D2: Kiểm tra người chấm phải là GVPB của đề tài hoặc thành viên hội đồng
        boolean isReviewer = councilTopic.getReviewerLecturer() != null
                && councilTopic.getReviewerLecturer().getId().equals(evaluator.getId());
        Optional<CouncilMember> memberOpt = memberRepository.findByCouncilIdAndLecturerId(council.getId(), evaluator.getId());
        boolean isMember = memberOpt.isPresent();

        if (!isReviewer && !isMember) {
            throw new IllegalArgumentException("Bạn không thuộc hội đồng này hoặc không phải GVPB của đề tài!");
        }

        // FB6: Tự động xác định evalType nếu không truyền vào (null)
        Evaluation.EvaluationType effectiveEvalType = evalType;
        if (effectiveEvalType == null) {
            if (isReviewer) {
                effectiveEvalType = Evaluation.EvaluationType.REVIEWER;
            } else if (memberOpt.isPresent() && memberOpt.get().getPosition() == CouncilMember.CouncilPosition.CHAIR) {
                effectiveEvalType = Evaluation.EvaluationType.COUNCIL_CHAIR;
            } else {
                effectiveEvalType = Evaluation.EvaluationType.COUNCIL_MEMBER;
            }
        }

        // Tính tổng điểm của GV này (Thang 10: 30% báo cáo, 40% sản phẩm, 30% thuyết trình)
        BigDecimal totalScore = c1.multiply(new BigDecimal("0.30"))
                .add(c2.multiply(new BigDecimal("0.40")))
                .add(c3.multiply(new BigDecimal("0.30")))
                .setScale(2, RoundingMode.HALF_UP);

        Evaluation eval = evaluationRepository.findByCouncilTopicIdAndEvaluatorLecturerId(councilTopicId, evaluator.getId())
                .orElse(Evaluation.builder()
                        .councilTopic(councilTopic)
                        .evaluatorLecturer(evaluator)
                        .build());

        eval.setEvaluationType(effectiveEvalType);
        eval.setCriteria1Score(c1);
        eval.setCriteria2Score(c2);
        eval.setCriteria3Score(c3);
        eval.setTotalScore(totalScore);
        eval.setComments(comments);
        eval.setGradedAt(LocalDateTime.now());

        Evaluation savedEval = evaluationRepository.save(eval);

        // Tự động tính lại điểm trung bình cộng của các thành viên trong hội đồng
        recalculateFinalScore(councilTopic);

        return savedEval;
    }

    @Transactional
    public void recalculateFinalScore(CouncilTopic councilTopic) {
        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_calculate_final_council_score");
            query.registerStoredProcedureParameter("p_council_topic_id", Long.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_final_score", BigDecimal.class, ParameterMode.OUT);
            query.setParameter("p_council_topic_id", councilTopic.getId());
            query.execute();

            BigDecimal finalScore = (BigDecimal) query.getOutputParameterValue("p_final_score");
            if (finalScore != null) {
                councilTopic.setFinalCouncilScore(finalScore);
                if (councilTopic.getGroup() != null) {
                    councilTopic.getGroup().setFinalScore(finalScore);
                }
            }
        } catch (Exception e) {
            // Fallback sang tính toán Java nếu môi trường không hỗ trợ Stored Procedure
            List<Evaluation> evals = evaluationRepository.findByCouncilTopicId(councilTopic.getId());
            if (!evals.isEmpty()) {
                BigDecimal sum = BigDecimal.ZERO;
                for (Evaluation eval : evals) {
                    sum = sum.add(eval.getTotalScore());
                }
                BigDecimal average = sum.divide(new BigDecimal(evals.size()), 2, RoundingMode.HALF_UP);
                councilTopic.setFinalCouncilScore(average);
                councilTopicRepository.save(councilTopic);

                StudentGroup group = councilTopic.getGroup();
                if (group != null) {
                    group.setFinalScore(average);
                    groupRepository.save(group);
                }
            }
        }
    }

    @Transactional
    @AuditAction(action = "PUBLISH_RESULTS", entityName = "CouncilTopic")
    public void publishResults(Long councilTopicId) {
        CouncilTopic councilTopic = getCouncilTopicById(councilTopicId);
        Council council = councilTopic.getCouncil();
        long memberCount = memberRepository.countByCouncilId(council.getId());
        long evalCount = evaluationRepository.countByCouncilTopicId(councilTopicId);

        if (councilTopic.getFinalCouncilScore() == null || evalCount < memberCount) {
            throw new IllegalStateException("Chưa thể công bố: Đề tài chưa hoàn tất chấm điểm bởi TẤT CẢ thành viên hội đồng, không thể công bố!");
        }

        try {
            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("sp_publish_council_results");
            query.registerStoredProcedureParameter("p_council_topic_id", Long.class, ParameterMode.IN);
            query.setParameter("p_council_topic_id", councilTopicId);
            query.execute();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }

        councilTopic.setIsPublished(true);
        councilTopic.setPublishedAt(LocalDateTime.now());
        councilTopicRepository.save(councilTopic);

        StudentGroup group = councilTopic.getGroup();
        if (group != null) {
            group.setStatus(StudentGroup.GroupStatus.COMPLETED);
            groupRepository.save(group);
            if (group.getTopic() != null) {
                group.getTopic().setStatus(Topic.TopicStatus.COMPLETED);
                topicRepository.save(group.getTopic());
            }
        }
    }
}
