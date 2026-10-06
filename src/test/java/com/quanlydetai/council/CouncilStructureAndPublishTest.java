package com.quanlydetai.council;

import com.quanlydetai.entity.*;
import com.quanlydetai.entity.RegistrationPeriod.PeriodType;
import com.quanlydetai.repository.*;
import com.quanlydetai.service.CouncilService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD - Phase 3 - Council Structure & Publish Rights
 * Seam: CouncilService.addCouncilMember (duplicate CHAIR/SECRETARY guard)
 *       CouncilService.isChairOfCouncil (query method)
 */
@SpringBootTest
@Transactional
class CouncilStructureAndPublishTest {

    @Autowired private CouncilService councilService;
    @Autowired private UserRepository userRepository;
    @Autowired private RegistrationPeriodRepository periodRepository;
    @Autowired private CouncilRepository councilRepository;
    @Autowired private CouncilMemberRepository memberRepository;

    private Council testCouncil;
    private List<User> lecturers;

    @BeforeEach
    void setUp() {
        lecturers = userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_LECTURER"))
                .limit(5)
                .toList();

        if (lecturers.size() < 2) {
            throw new IllegalStateException("Need at least 2 LECTURERs in DB");
        }

        User creator = lecturers.get(0);
        RegistrationPeriod period = RegistrationPeriod.builder()
                .periodName("Council Test Period")
                .periodType(PeriodType.GRADUATION_THESIS)
                .academicYear("2026-2027")
                .semester(1)
                .topicSubmissionStart(LocalDateTime.now().minusDays(10))
                .topicSubmissionEnd(LocalDateTime.now().minusDays(5))
                .studentRegistrationStart(LocalDateTime.now().minusDays(4))
                .studentRegistrationEnd(LocalDateTime.now().plusDays(5))
                .reviewDeadline(LocalDateTime.now().plusDays(20))
                .defenseDate(LocalDateTime.now().plusDays(30).toLocalDate())
                .createdBy(creator)
                .build();
        periodRepository.save(period);

        testCouncil = councilService.createCouncil(
                "COUNCIL_TEST", "Test Council", period.getId(),
                LocalDateTime.now().plusDays(30), "Phong B201");
    }

    // ====================================================================
    // SEAM 1: Khong duoc co 2 CHAIR trong cung 1 hoi dong
    // ====================================================================
    @Test
    void addCouncilMember_DuplicateChair_ThrowsException() {
        councilService.addCouncilMember(testCouncil.getId(), lecturers.get(0).getId(),
                CouncilMember.CouncilPosition.CHAIR);

        assertThatThrownBy(() ->
                councilService.addCouncilMember(testCouncil.getId(), lecturers.get(1).getId(),
                        CouncilMember.CouncilPosition.CHAIR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CHỦ TỊCH");
    }

    // ====================================================================
    // SEAM 2: Khong duoc co 2 SECRETARY trong cung 1 hoi dong
    // ====================================================================
    @Test
    void addCouncilMember_DuplicateSecretary_ThrowsException() {
        councilService.addCouncilMember(testCouncil.getId(), lecturers.get(0).getId(),
                CouncilMember.CouncilPosition.SECRETARY);

        assertThatThrownBy(() ->
                councilService.addCouncilMember(testCouncil.getId(), lecturers.get(1).getId(),
                        CouncilMember.CouncilPosition.SECRETARY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("THU KY");
    }

    // ====================================================================
    // SEAM 3: Them thanh vien khac nhau chuc vu -> OK
    // ====================================================================
    @Test
    void addCouncilMember_DifferentPositions_Succeeds() {
        councilService.addCouncilMember(testCouncil.getId(), lecturers.get(0).getId(),
                CouncilMember.CouncilPosition.CHAIR);
        councilService.addCouncilMember(testCouncil.getId(), lecturers.get(1).getId(),
                CouncilMember.CouncilPosition.SECRETARY);

        long count = memberRepository.countByCouncilId(testCouncil.getId());
        assertThat(count).isEqualTo(2);
    }

    // ====================================================================
    // SEAM 4: isChairOfCouncil() - kiem tra dung
    // ====================================================================
    @Test
    void isChairOfCouncil_WhenUserIsChair_ReturnsTrue() {
        councilService.addCouncilMember(testCouncil.getId(), lecturers.get(0).getId(),
                CouncilMember.CouncilPosition.CHAIR);

        assertThat(councilService.isChairOfCouncil(testCouncil.getId(), lecturers.get(0).getId()))
                .isTrue();
    }

    @Test
    void isChairOfCouncil_WhenUserIsNotChair_ReturnsFalse() {
        councilService.addCouncilMember(testCouncil.getId(), lecturers.get(0).getId(),
                CouncilMember.CouncilPosition.CHAIR);

        assertThat(councilService.isChairOfCouncil(testCouncil.getId(), lecturers.get(1).getId()))
                .isFalse();
    }

    // ====================================================================
    // SEAM 5: removeCouncilMember - Xoa thanh vien khoi hoi dong
    // ====================================================================
    @Test
    void removeCouncilMember_Succeeds() {
        councilService.addCouncilMember(testCouncil.getId(), lecturers.get(0).getId(),
                CouncilMember.CouncilPosition.MEMBER);
        assertThat(memberRepository.countByCouncilId(testCouncil.getId())).isEqualTo(1);

        councilService.removeCouncilMember(testCouncil.getId(), lecturers.get(0).getId());
        assertThat(memberRepository.countByCouncilId(testCouncil.getId())).isEqualTo(0);
    }

    @Test
    void removeCouncilMember_NotFound_ThrowsException() {
        assertThatThrownBy(() ->
                councilService.removeCouncilMember(testCouncil.getId(), 999999L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}