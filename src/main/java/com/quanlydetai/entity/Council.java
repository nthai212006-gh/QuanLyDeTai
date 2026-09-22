package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "councils")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Council {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "council_code", unique = true, nullable = false, length = 50)
    private String councilCode;

    @Column(name = "council_name", nullable = false, length = 150)
    private String councilName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private RegistrationPeriod period;

    @Column(name = "defense_date")
    private LocalDateTime defenseDate;

    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('CREATED', 'IN_SESSION', 'COMPLETED')")
    @Builder.Default
    private CouncilStatus status = CouncilStatus.CREATED;

    @OneToMany(mappedBy = "council", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CouncilMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "council", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CouncilTopic> assignedTopics = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum CouncilStatus {
        CREATED, IN_SESSION, COMPLETED;

        public static final CouncilStatus IN_PROGRESS = IN_SESSION;
    }
}
