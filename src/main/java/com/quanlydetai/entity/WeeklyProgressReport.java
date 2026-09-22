package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_progress_reports", uniqueConstraints = {
    @UniqueConstraint(name = "uk_group_week", columnNames = {"group_id", "week_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyProgressReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "tasks_completed", nullable = false, columnDefinition = "TEXT")
    private String tasksCompleted;

    @Column(name = "issues_faced", columnDefinition = "TEXT")
    private String issuesFaced;

    @Column(name = "next_week_plan", nullable = false, columnDefinition = "TEXT")
    private String nextWeekPlan;

    @Column(name = "report_file_url", length = 255)
    private String reportFileUrl;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "lecturer_feedback", columnDefinition = "TEXT")
    private String lecturerFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_status", columnDefinition = "ENUM('PENDING', 'ACCEPTED', 'NEEDS_REVISION')")
    @Builder.Default
    private FeedbackStatus feedbackStatus = FeedbackStatus.PENDING;

    @Column(name = "feedback_at")
    private LocalDateTime feedbackAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    // Backward compatibility helpers
    @Transient
    private LocalDate startDate;

    @Transient
    private LocalDate endDate;

    @Transient
    private User submittedBy;

    @Transient
    public String getDifficulties() {
        return issuesFaced;
    }

    public void setDifficulties(String difficulties) {
        this.issuesFaced = difficulties;
    }

    @Transient
    public String getAttachmentUrl() {
        return reportFileUrl;
    }

    public void setAttachmentUrl(String url) {
        this.reportFileUrl = url;
    }

    public enum FeedbackStatus {
        PENDING, ACCEPTED, NEEDS_REVISION;

        public static final FeedbackStatus APPROVED = ACCEPTED;
        public static final FeedbackStatus REVISION_REQUIRED = NEEDS_REVISION;
    }

    public enum ReportStatus {
        PENDING, ACCEPTED, NEEDS_REVISION, APPROVED, REVISION_REQUIRED;

        public FeedbackStatus toFeedbackStatus() {
            if (this == APPROVED) return FeedbackStatus.ACCEPTED;
            if (this == REVISION_REQUIRED) return FeedbackStatus.NEEDS_REVISION;
            return FeedbackStatus.valueOf(this.name());
        }
    }

    @Transient
    public FeedbackStatus getStatus() {
        return feedbackStatus;
    }

    public void setStatus(FeedbackStatus status) {
        this.feedbackStatus = status;
    }

    public void setStatus(ReportStatus status) {
        if (status != null) {
            this.feedbackStatus = status.toFeedbackStatus();
        }
    }

    @Transient
    public LocalDateTime getReviewedAt() {
        return feedbackAt;
    }

    public void setReviewedAt(LocalDateTime dt) {
        this.feedbackAt = dt;
    }
}
