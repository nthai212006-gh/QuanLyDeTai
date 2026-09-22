package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "link", length = 255)
    private String link;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private NotificationType type = NotificationType.SYSTEM;

    @Transient
    @Builder.Default
    private Boolean emailSent = false;

    // Backward compatibility helper methods
    @Transient
    public String getMessage() {
        return content;
    }

    public void setMessage(String message) {
        this.content = message;
    }

    @Transient
    public String getReferenceUrl() {
        return link;
    }

    public void setReferenceUrl(String referenceUrl) {
        this.link = referenceUrl;
    }

    public enum NotificationType {
        TOPIC_REGISTRATION, DEADLINE_REMINDER, WEEKLY_REPORT, COUNCIL_SCHEDULE, GRADE_PUBLISHED, SYSTEM
    }

    public static class NotificationBuilder {
        public NotificationBuilder message(String message) {
            this.content = message;
            return this;
        }

        public NotificationBuilder referenceUrl(String refUrl) {
            this.link = refUrl;
            return this;
        }
    }
}
