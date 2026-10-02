package com.quanlydetai.service;

import com.quanlydetai.entity.GroupMember;
import com.quanlydetai.entity.RegistrationPeriod;
import com.quanlydetai.entity.StudentGroup;
import com.quanlydetai.entity.User;
import com.quanlydetai.repository.GroupMemberRepository;
import com.quanlydetai.repository.RegistrationPeriodRepository;
import com.quanlydetai.repository.StudentGroupRepository;
import com.quanlydetai.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class GroupSelectionAndCapacityTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository memberRepository;

    @Autowired
    private RegistrationPeriodRepository periodRepository;

    @Test
    @DisplayName("KỊCH BẢN 4: Group already has 3 members -> adding 4th member throws friendly rule exception")
    void testGroupCapacityLimitEnforcedByService() {
        // Group 1 already has SV001, SV002, SV003 from seed data
        StudentGroup group1 = groupRepository.findById(1L).orElseThrow();
        assertThat(memberRepository.countByGroupId(group1.getId())).isEqualTo(3L);

        // Try adding a new student code (or any code)
        assertThatThrownBy(() -> groupService.addMember(group1.getId(), "SV004"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Quy định đề án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!");
    }

    @Test
    @DisplayName("Student period resolution: finds active period and joined periods correctly")
    void testStudentPeriodResolution() {
        User sv001 = userRepository.findByUserCode("SV001").orElseThrow();

        // Check joined period IDs for SV001 contains Period 1
        Set<Long> joinedPeriodIds = groupService.getJoinedPeriodIdsByStudent(sv001.getId());
        assertThat(joinedPeriodIds).isNotEmpty();
        assertThat(joinedPeriodIds).contains(1L);

        // Check active or latest period resolution for SV001
        Optional<Long> activePeriodId = groupService.findActivePeriodIdByStudent(sv001.getId());
        Optional<Long> latestPeriodId = groupService.findLatestPeriodIdByStudent(sv001.getId());

        // Either active period or latest period must resolve to Period 1
        Long resolvedPeriodId = activePeriodId.or(() -> latestPeriodId).orElse(null);
        assertThat(resolvedPeriodId).isEqualTo(1L);
    }
}
