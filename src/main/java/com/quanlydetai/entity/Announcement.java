package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Transient
    @Builder.Default
    private AnnouncementCategory category = AnnouncementCategory.FACULTY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "is_pinned")
    @Builder.Default
    private Boolean isPinned = false;

    @Transient
    private String attachmentUrl;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private LocalDateTime updatedAt;

    public enum AnnouncementCategory {
        FACULTY, UNIVERSITY, PERIOD_NOTIFICATION
    }
}
