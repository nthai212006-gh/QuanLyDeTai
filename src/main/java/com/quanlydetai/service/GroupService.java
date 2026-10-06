package com.quanlydetai.service;

import com.quanlydetai.aspect.AuditAction;
import com.quanlydetai.entity.*;
import com.quanlydetai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final TopicRepository topicRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public List<StudentGroup> getGroupsByPeriod(Long periodId) {
        return groupRepository.findByPeriodId(periodId);
    }

    public StudentGroup getGroupById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm ID: " + id));
    }

    public Optional<StudentGroup> getGroupByStudentAndPeriod(Long studentId, Long periodId) {
        return groupRepository.findGroupByStudentAndPeriod(studentId, periodId);
    }

    public Optional<Long> findActivePeriodIdByStudent(Long studentId) {
        List<GroupMember> members = memberRepository.findMembersWithPeriodByStudentId(studentId);
        // Ưu tiên 1: Đợt mà nhóm của SV đang hoạt động (chưa hoàn thành)
        Optional<Long> activeGroupPeriod = members.stream()
                .filter(gm -> gm.getGroup() != null
                        && gm.getGroup().getStatus() != StudentGroup.GroupStatus.COMPLETED
                        && gm.getGroup().getStatus() != StudentGroup.GroupStatus.DISQUALIFIED)
                .map(GroupMember::getPeriod)
                .filter(java.util.Objects::nonNull)
                .map(RegistrationPeriod::getId)
                .findFirst();

        if (activeGroupPeriod.isPresent()) {
            return activeGroupPeriod;
        }

        // Ưu tiên 2: Đợt đang trong giai đoạn SV đăng ký hoặc thực hiện
        return members.stream()
                .map(GroupMember::getPeriod)
                .filter(p -> p != null && (p.getStatus() == RegistrationPeriod.PeriodStatus.SV_REGISTRATION || p.getStatus() == RegistrationPeriod.PeriodStatus.IN_PROGRESS))
                .map(RegistrationPeriod::getId)
                .findFirst();
    }

    public Optional<Long> findLatestPeriodIdByStudent(Long studentId) {
        List<GroupMember> members = memberRepository.findMembersWithPeriodByStudentId(studentId);
        return members.stream()
                .map(GroupMember::getPeriod)
                .filter(java.util.Objects::nonNull)
                .map(RegistrationPeriod::getId)
                .findFirst();
    }

    public java.util.Set<Long> getJoinedPeriodIdsByStudent(Long studentId) {
        return memberRepository.findJoinedPeriodIdsByStudentId(studentId);
    }

    public Long getPeriodIdByGroupId(Long groupId) {
        return getGroupById(groupId).getPeriod().getId();
    }

    @Transactional
    @AuditAction(action = "CREATE_GROUP", entityName = "StudentGroup")
    public StudentGroup createGroup(String groupName, Long periodId, User leader) {
        // Kiểm tra xem sinh viên đã tham gia nhóm nào trong đợt này chưa
        if (memberRepository.existsByStudentIdAndPeriodId(leader.getId(), periodId)) {
            throw new IllegalStateException("Bạn đã tham gia một nhóm khác trong đợt đăng ký này!");
        }

        // Quy tắc: Mỗi SV chỉ được tham gia duy nhất một nhóm trong suốt quá trình thực hiện đề tài (chưa hoàn thành)
        if (memberRepository.hasActiveGroupMembership(leader.getId(), List.of(StudentGroup.GroupStatus.COMPLETED, StudentGroup.GroupStatus.DISQUALIFIED))) {
            throw new IllegalStateException("Bạn đang tham gia một nhóm đề tài khác chưa hoàn thành!");
        }

        RegistrationPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký"));

        String groupCode = "GRP_" + period.getId() + "_" + System.currentTimeMillis() % 10000;

        StudentGroup group = StudentGroup.builder()
                .groupCode(groupCode)
                .groupName(groupName)
                .period(period)
                .leader(leader)
                .status(StudentGroup.GroupStatus.FORMING)
                .registrationStatus(StudentGroup.RegistrationStatus.NONE)
                .build();

        StudentGroup savedGroup = groupRepository.save(group);

        // Thêm leader vào bảng group_members
        GroupMember leaderMember = GroupMember.builder()
                .group(savedGroup)
                .student(leader)
                .period(period)
                .roleInGroup(GroupMember.RoleInGroup.LEADER)
                .build();
        memberRepository.save(leaderMember);

        return savedGroup;
    }

    @Transactional
    @AuditAction(action = "ADD_MEMBER", entityName = "GroupMember")
    public void addMember(Long groupId, String studentCode) {
        StudentGroup group = getGroupById(groupId);
        long currentMembersCount = memberRepository.countByGroupId(groupId);
        if (currentMembersCount >= 3) {
            throw new IllegalStateException("Quy định đề án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!");
        }

        User student = userRepository.findByUserCode(studentCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên với mã: " + studentCode));

        if (memberRepository.existsByStudentIdAndPeriodId(student.getId(), group.getPeriod().getId())) {
            throw new IllegalStateException("Sinh viên " + student.getFullName() + " đã ở trong một nhóm khác của đợt này!");
        }

        // Quy tắc: Mỗi SV chỉ được tham gia duy nhất một nhóm trong suốt quá trình thực hiện đề tài (chưa hoàn thành)
        if (memberRepository.hasActiveGroupMembership(student.getId(), List.of(StudentGroup.GroupStatus.COMPLETED, StudentGroup.GroupStatus.DISQUALIFIED))) {
            throw new IllegalStateException("Sinh viên " + student.getFullName() + " đang tham gia một nhóm đề tài khác chưa hoàn thành!");
        }

        GroupMember newMember = GroupMember.builder()
                .group(group)
                .student(student)
                .period(group.getPeriod())
                .roleInGroup(GroupMember.RoleInGroup.MEMBER)
                .build();
        memberRepository.save(newMember);
    }

    @Transactional
    @AuditAction(action = "REGISTER_TOPIC", entityName = "StudentGroup")
    public void registerTopic(Long groupId, Long topicId, User requester) {
        StudentGroup group = getGroupById(groupId);

        // Chỉ nhóm trưởng mới được đăng ký đề tài
        if (!group.getLeader().getId().equals(requester.getId())) {
            throw new IllegalStateException("Chỉ nhóm trưởng mới có quyền đăng ký đề tài!");
        }

        if (group.getTopic() != null) {
            throw new IllegalStateException("Nhóm đã đăng ký đề tài rồi. Mỗi nhóm chỉ được nhận duy nhất 1 đề tài!");
        }

        // RÀNG BUỘC GIAI ĐOẠN 2: SV chỉ được đăng ký đề tài trong cửa sổ thời gian SV
        if (!group.getPeriod().isStudentRegistrationOpen()) {
            throw new IllegalStateException(
                    "Giai đoạn SV đăng ký đề tài chưa mở hoặc đã kết thúc. Vui lòng kiểm tra lịch đợt đăng ký.");
        }

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài"));

        if (topic.getStatus() != Topic.TopicStatus.APPROVED) {
            throw new IllegalStateException("Đề tài này chưa được duyệt hoặc đã có nhóm khác nhận!");
        }

        group.setTopic(topic);
        group.setRegistrationStatus(StudentGroup.RegistrationStatus.PENDING);
        group.setStatus(StudentGroup.GroupStatus.REGISTERED);
        groupRepository.save(group);
    }

    @Transactional
    @AuditAction(action = "APPROVE_GROUP_TOPIC", entityName = "StudentGroup")
    public void approveTopicRegistration(Long groupId, boolean approve) {
        StudentGroup group = getGroupById(groupId);
        if (approve) {
            group.setRegistrationStatus(StudentGroup.RegistrationStatus.APPROVED);
            group.setStatus(StudentGroup.GroupStatus.DOING);
            if (group.getTopic() != null) {
                group.getTopic().setStatus(Topic.TopicStatus.ASSIGNED);
                topicRepository.save(group.getTopic());
            }
        } else {
            group.setRegistrationStatus(StudentGroup.RegistrationStatus.REJECTED);
            group.setTopic(null);
            group.setStatus(StudentGroup.GroupStatus.FORMING);
        }
        groupRepository.save(group);
    }

    @Transactional
    @AuditAction(action = "SUBMIT_REPORT", entityName = "TopicSubmission")
    public TopicSubmission submitReport(Long groupId, User submitter, String title, String fileUrl, String sourceCodeUrl, String note) {
        StudentGroup group = getGroupById(groupId);

        // Kiểm tra chặt chẽ: Chỉ nhóm trưởng nộp báo cáo
        if (!group.getLeader().getId().equals(submitter.getId())) {
            throw new IllegalStateException("Quy định: Việc nộp báo cáo đề tài chỉ được thực hiện bởi nhóm trưởng!");
        }

        TopicSubmission submission = TopicSubmission.builder()
                .group(group)
                .submittedBy(submitter)
                .title(title)
                .fileUrl(fileUrl)
                .sourceCodeUrl(sourceCodeUrl)
                .note(note)
                .submissionRound(1)
                .submittedAt(LocalDateTime.now())
                .build();

        group.setStatus(StudentGroup.GroupStatus.SUBMITTED);
        groupRepository.save(group);

        return submissionRepository.save(submission);
    }

    public List<TopicSubmission> getSubmissionsByGroup(Long groupId) {
        return submissionRepository.findByGroupIdOrderBySubmittedAtDesc(groupId);
    }

    @Transactional
    @AuditAction(action = "REMOVE_MEMBER", entityName = "StudentGroup")
    public void removeMember(Long groupId, Long studentId) {
        StudentGroup group = getGroupById(groupId);
        GroupMember memberToRemove = memberRepository.findByGroupIdAndStudentId(groupId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Sinh viên không thuộc nhóm này"));

        memberRepository.delete(memberToRemove);
        long remainingCount = memberRepository.countByGroupId(groupId);

        if (remainingCount == 0) {
            // CASE 1: Nhóm giải thể hoàn toàn -> Giải phóng đề tài trước, set topic_id = null sau
            if (group.getTopic() != null) {
                Topic topic = group.getTopic();
                topic.setStatus(Topic.TopicStatus.APPROVED);
                topicRepository.save(topic);
                group.setTopic(null);
            }
            group.setStatus(StudentGroup.GroupStatus.DISQUALIFIED);
            groupRepository.save(group);
        } else if (group.getLeader().getId().equals(studentId)) {
            // CASE 2: Leader rút nhưng nhóm còn người -> Chuyển quyền cho người tham gia sớm nhất
            GroupMember newLeader = memberRepository.findFirstByGroupIdOrderByJoinedAtAsc(groupId)
                    .orElseThrow();
            newLeader.setRoleInGroup(GroupMember.RoleInGroup.LEADER);
            memberRepository.save(newLeader);

            group.setLeader(newLeader.getStudent());
            groupRepository.save(group);
        }
    }
}
