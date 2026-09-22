package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "query_input", nullable = false, columnDefinition = "TEXT")
    private String queryInput;

    @Column(name = "ai_output", nullable = false, columnDefinition = "TEXT")
    private String aiOutput;

    @Column(name = "similarity_score", precision = 5, scale = 2)
    private BigDecimal similarityScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private RegistrationPeriod period;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Backward compatibility helper methods
    @Transient
    public String getInputContent() {
        return queryInput;
    }

    public void setInputContent(String inputContent) {
        this.queryInput = inputContent;
    }

    @Transient
    public String getAiResponse() {
        return aiOutput;
    }

    public void setAiResponse(String aiResponse) {
        this.aiOutput = aiResponse;
    }

    public enum AnalysisType {
        TOPIC_RECOMMENDATION, TOPIC_SIMILARITY, REPORT_SUMMARY
    }

    public static class AiAnalysisLogBuilder {
        public AiAnalysisLogBuilder analysisType(AnalysisType type) {
            this.actionType = type != null ? type.name() : null;
            return this;
        }

        public AiAnalysisLogBuilder inputContent(String input) {
            this.queryInput = input;
            return this;
        }

        public AiAnalysisLogBuilder aiResponse(String response) {
            this.aiOutput = response;
            return this;
        }
    }
}
