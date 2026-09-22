package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_student_period", columnNames = {"student_id", "period_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_group", columnDefinition = "ENUM('LEADER', 'MEMBER')")
    @Builder.Default
    private RoleInGroup roleInGroup = RoleInGroup.MEMBER;

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public enum RoleInGroup {
        LEADER, MEMBER
    }
}
