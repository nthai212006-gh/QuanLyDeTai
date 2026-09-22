package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "council_topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouncilTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private StudentGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_lecturer_id")
    private User reviewerLecturer;

    @Column(name = "defense_order")
    private Integer defenseOrder;

    @Column(name = "final_council_score", precision = 4, scale = 2)
    private BigDecimal finalCouncilScore;

    @Transient
    private String councilNotes;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @Transient
    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "councilTopic", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Evaluation> evaluations = new ArrayList<>();
}
