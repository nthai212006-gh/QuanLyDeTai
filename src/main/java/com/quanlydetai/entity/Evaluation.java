package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"council_topic_id", "evaluator_lecturer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_topic_id", nullable = false)
    private CouncilTopic councilTopic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_lecturer_id", nullable = false)
    private User evaluatorLecturer;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_type", nullable = false, columnDefinition = "ENUM('INSTRUCTOR', 'REVIEWER', 'COUNCIL_MEMBER', 'COUNCIL_CHAIR')")
    private EvaluationType evaluationType;

    @Column(name = "criteria_1_score", precision = 4, scale = 2)
    private BigDecimal criteria1Score;

    @Column(name = "criteria_2_score", precision = 4, scale = 2)
    private BigDecimal criteria2Score;

    @Column(name = "criteria_3_score", precision = 4, scale = 2)
    private BigDecimal criteria3Score;

    @Column(name = "total_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal totalScore;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "graded_at", insertable = false, updatable = false)
    private LocalDateTime gradedAt;

    public enum EvaluationType {
        INSTRUCTOR, REVIEWER, COUNCIL_MEMBER, COUNCIL_CHAIR
    }
}
