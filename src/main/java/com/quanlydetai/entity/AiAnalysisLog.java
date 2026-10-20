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
public class AiAnalysisLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private RegistrationPeriod period;

    @Column(name = "query_input", nullable = false, columnDefinition = "TEXT")
    private String queryInput;

    @Column(name = "ai_output", nullable = false, columnDefinition = "TEXT")
    private String aiOutput;

    @Column(name = "similarity_score", precision = 5, scale = 2)
    private BigDecimal similarityScore;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Aliases for compatibility
    public String getAnalysisType() {
        return actionType;
    }

    public void setAnalysisType(String analysisType) {
        this.actionType = analysisType;
    }

    public String getInputContent() {
        return queryInput;
    }

    public void setInputContent(String inputContent) {
        this.queryInput = inputContent;
    }

    public String getAiResponse() {
        return aiOutput;
    }

    public void setAiResponse(String aiResponse) {
        this.aiOutput = aiResponse;
    }

    public enum AnalysisType {
        TOPIC_RECOMMENDATION, TOPIC_SIMILARITY, REPORT_SUMMARY
    }

    public static AiAnalysisLogBuilder builder() {
        return new AiAnalysisLogBuilder();
    }

    public static class AiAnalysisLogBuilder {
        private Long id;
        private User user;
        private String actionType;
        private RegistrationPeriod period;
        private String queryInput;
        private String aiOutput;
        private BigDecimal similarityScore;
        private LocalDateTime createdAt;

        AiAnalysisLogBuilder() {}

        public AiAnalysisLogBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AiAnalysisLogBuilder user(User user) {
            this.user = user;
            return this;
        }

        public AiAnalysisLogBuilder actionType(String actionType) {
            this.actionType = actionType;
            return this;
        }

        public AiAnalysisLogBuilder analysisType(AnalysisType type) {
            this.actionType = type != null ? type.name() : null;
            return this;
        }

        public AiAnalysisLogBuilder analysisType(String analysisType) {
            this.actionType = analysisType;
            return this;
        }

        public AiAnalysisLogBuilder period(RegistrationPeriod period) {
            this.period = period;
            return this;
        }

        public AiAnalysisLogBuilder queryInput(String queryInput) {
            this.queryInput = queryInput;
            return this;
        }

        public AiAnalysisLogBuilder inputContent(String inputContent) {
            this.queryInput = inputContent;
            return this;
        }

        public AiAnalysisLogBuilder aiOutput(String aiOutput) {
            this.aiOutput = aiOutput;
            return this;
        }

        public AiAnalysisLogBuilder aiResponse(String aiResponse) {
            this.aiOutput = aiResponse;
            return this;
        }

        public AiAnalysisLogBuilder similarityScore(BigDecimal similarityScore) {
            this.similarityScore = similarityScore;
            return this;
        }

        public AiAnalysisLogBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AiAnalysisLog build() {
            AiAnalysisLog log = new AiAnalysisLog();
            log.id = this.id;
            log.user = this.user;
            log.actionType = this.actionType;
            log.period = this.period;
            log.queryInput = this.queryInput;
            log.aiOutput = this.aiOutput;
            log.similarityScore = this.similarityScore;
            log.createdAt = this.createdAt;
            return log;
        }
    }
}
