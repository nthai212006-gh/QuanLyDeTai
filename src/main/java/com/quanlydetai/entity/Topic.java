package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    @org.hibernate.annotations.BatchSize(size = 25)
    @Builder.Default
    private List<TopicSupervisor> supervisors = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum TopicStatus {
        PENDING_APPROVAL, APPROVED, REJECTED, ASSIGNED, COMPLETED
    }

    @Transient
    public User getPrimarySupervisor() {
        if (supervisors != null && !supervisors.isEmpty()) {
            for (TopicSupervisor ts : supervisors) {
                if (Boolean.TRUE.equals(ts.getIsPrimary())) {
                    return ts.getLecturer();
                }
            }
            log.warn("Dữ liệu bất thường: Topic ID {} có {} supervisors nhưng không có ai mang is_primary = true. Fallback về createdByLecturer.", id, supervisors.size());
        }
        return createdByLecturer;
    }

    @Transient
    public User getCoSupervisor() {
        if (supervisors != null && !supervisors.isEmpty()) {
            for (TopicSupervisor ts : supervisors) {
                if (Boolean.FALSE.equals(ts.getIsPrimary())) {
                    return ts.getLecturer();
                }
            }
        }
        return null;
    }

    @Transient
    public boolean hasCoSupervisor() {
        return getCoSupervisor() != null;
    }

    @Transient
    public String getSupervisorsDisplayString() {
        User primary = getPrimarySupervisor();
        String primaryName = primary != null ? primary.getFullName() : (createdByLecturer != null ? createdByLecturer.getFullName() : "N/A");
        User co = getCoSupervisor();
        if (co != null) {
            return primaryName + " (Chính), " + co.getFullName() + " (Đồng HD)";
        }
        return primaryName;
    }
}
