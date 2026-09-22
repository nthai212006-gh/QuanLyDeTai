package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "student_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING_APPROVAL', 'ASSIGNED', 'IN_PROGRESS', 'DEFENDING', 'COMPLETED', 'DISQUALIFIED')")
    @Builder.Default
    private GroupStatus status = GroupStatus.PENDING_APPROVAL;

    @Column(name = "final_score", precision = 4, scale = 2)
    private BigDecimal finalScore;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GroupMember> members = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Backward compatibility helper fields/methods
    @Transient
    private String groupCode;

    public String getGroupCode() {
        if (groupCode != null) return groupCode;
        return "GRP_" + (id != null ? String.format("%02d", id) : "01");
    }

    public void setGroupCode(String code) {
        this.groupCode = code;
    }

    @Transient
    @Builder.Default
    private RegistrationStatus registrationStatus = RegistrationStatus.NONE;

    public enum RegistrationStatus {
        NONE, PENDING, APPROVED, REJECTED
    }

    public enum GroupStatus {
        PENDING_APPROVAL, ASSIGNED, IN_PROGRESS, DEFENDING, COMPLETED, DISQUALIFIED;

        // Compatibility constants
        public static final GroupStatus FORMING = PENDING_APPROVAL;
        public static final GroupStatus REGISTERED = ASSIGNED;
        public static final GroupStatus DOING = IN_PROGRESS;
        public static final GroupStatus SUBMITTED = IN_PROGRESS;
    }
}
