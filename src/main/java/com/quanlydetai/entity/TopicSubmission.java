package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private User submittedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "submission_type", nullable = false, columnDefinition = "ENUM('PROPOSAL', 'PROGRESS', 'FINAL_THESIS', 'SOURCE_CODE')")
    @Builder.Default
    private SubmissionType submissionType = SubmissionType.PROGRESS;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "file_url", length = 255)
    private String fileUrl;

    @Transient
    private String sourceCodeUrl;

    @Transient
    @Builder.Default
    private Integer submissionRound = 1;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "submission_time", insertable = false, updatable = false)
    private LocalDateTime submittedAt;

    public LocalDateTime getSubmissionTime() {
        return submittedAt;
    }

    public void setSubmissionTime(LocalDateTime dt) {
        this.submittedAt = dt;
    }

    public enum SubmissionType {
        PROPOSAL, PROGRESS, FINAL_THESIS, SOURCE_CODE
    }
}
