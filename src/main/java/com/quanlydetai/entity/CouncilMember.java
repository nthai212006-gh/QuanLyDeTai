package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "council_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"council_id", "lecturer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouncilMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('CHAIR', 'SECRETARY', 'MEMBER', 'REVIEWER')")
    private CouncilPosition position;

    public enum CouncilPosition {
        CHAIR, SECRETARY, MEMBER, REVIEWER
    }
}
