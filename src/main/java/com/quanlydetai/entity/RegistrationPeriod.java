package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registration_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_name", nullable = false, length = 150)
    private String periodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, columnDefinition = "ENUM('COURSE_PROJECT', 'RESEARCH', 'GRADUATION_THESIS', 'INTERNSHIP')")
    private PeriodType periodType;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "topic_submission_start", nullable = false)
    private LocalDateTime topicSubmissionStart;

    @Column(name = "topic_submission_end", nullable = false)
    private LocalDateTime topicSubmissionEnd;

    @Column(name = "student_registration_start", nullable = false)
    private LocalDateTime studentRegistrationStart;

    @Column(name = "student_registration_end", nullable = false)
    private LocalDateTime studentRegistrationEnd;

    @Column(name = "review_deadline")
    private LocalDateTime reviewDeadline;

    @Column(name = "defense_date")
    private LocalDate defenseDate;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum PeriodType {
        COURSE_PROJECT, RESEARCH, GRADUATION_THESIS, INTERNSHIP
    }

    public enum PeriodStatus {
        UPCOMING, GV_REGISTRATION, SV_REGISTRATION, IN_PROGRESS, DEFENSE, COMPLETED, CLOSED
    }

    // Backward compatibility helper getters/setters
    @Transient
    public LocalDateTime getGvStartDate() {
        return topicSubmissionStart;
    }

    public void setGvStartDate(LocalDateTime dt) {
        this.topicSubmissionStart = dt;
    }

    @Transient
    public LocalDateTime getGvEndDate() {
        return topicSubmissionEnd;
    }

    public void setGvEndDate(LocalDateTime dt) {
        this.topicSubmissionEnd = dt;
    }

    @Transient
    public LocalDateTime getSvStartDate() {
        return studentRegistrationStart;
    }

    public void setSvStartDate(LocalDateTime dt) {
        this.studentRegistrationStart = dt;
    }

    @Transient
    public LocalDateTime getSvEndDate() {
        return studentRegistrationEnd;
    }

    public void setSvEndDate(LocalDateTime dt) {
        this.studentRegistrationEnd = dt;
    }

    @Transient
    public LocalDateTime getGvpbDeadline() {
        return reviewDeadline;
    }

    public void setGvpbDeadline(LocalDateTime dt) {
        this.reviewDeadline = dt;
    }

    @Transient
    public LocalDateTime getCouncilDate() {
        return defenseDate != null ? defenseDate.atStartOfDay() : null;
    }

    public void setCouncilDate(LocalDateTime dt) {
        this.defenseDate = dt != null ? dt.toLocalDate() : null;
    }

    /** Giai đoạn 1: GV đang trong cửa sổ đề xuất đề tài */
    @Transient
    public boolean isTopicSubmissionOpen() {
        LocalDateTime now = LocalDateTime.now();
        return topicSubmissionStart != null && topicSubmissionEnd != null
                && !now.isBefore(topicSubmissionStart)
                && now.isBefore(topicSubmissionEnd);
    }

    /** Giai đoạn 2: SV đang trong cửa sổ đăng ký đề tài */
    @Transient
    public boolean isStudentRegistrationOpen() {
        LocalDateTime now = LocalDateTime.now();
        return studentRegistrationStart != null && studentRegistrationEnd != null
                && !now.isBefore(studentRegistrationStart)
                && now.isBefore(studentRegistrationEnd);
    }

    @Transient
    public PeriodStatus getStatus() {
        if (Boolean.FALSE.equals(isActive)) {
            return PeriodStatus.CLOSED;
        }
        LocalDateTime now = LocalDateTime.now();
        if (topicSubmissionStart != null && now.isBefore(topicSubmissionStart)) {
            return PeriodStatus.UPCOMING;
        }
        if (topicSubmissionEnd != null && now.isBefore(topicSubmissionEnd)) {
            return PeriodStatus.GV_REGISTRATION;
        }
        if (studentRegistrationEnd != null && now.isBefore(studentRegistrationEnd)) {
            return PeriodStatus.SV_REGISTRATION;
        }
        if (reviewDeadline != null && now.isBefore(reviewDeadline)) {
            return PeriodStatus.IN_PROGRESS;
        }
        return PeriodStatus.COMPLETED;
    }

    public void setStatus(PeriodStatus status) {
        this.isActive = (status != PeriodStatus.CLOSED);
    }
}
