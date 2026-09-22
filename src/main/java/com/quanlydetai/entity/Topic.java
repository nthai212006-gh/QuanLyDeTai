package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_code", unique = true, nullable = false, length = 50)
    private String topicCode;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(name = "max_groups")
    @Builder.Default
    private Integer maxGroups = 1;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ASSIGNED', 'COMPLETED')")
    @Builder.Default
    private TopicStatus status = TopicStatus.PENDING_APPROVAL;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_lecturer_id", nullable = false)
    private User createdByLecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TopicSupervisor> supervisors = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum TopicStatus {
        PENDING_APPROVAL, APPROVED, REJECTED, ASSIGNED, COMPLETED
    }
}
