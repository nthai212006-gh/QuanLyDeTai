package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "topic_supervisors")
@IdClass(TopicSupervisorId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicSupervisor {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = true;
}
