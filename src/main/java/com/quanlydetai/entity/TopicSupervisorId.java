package com.quanlydetai.entity;

import lombok.*;
import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TopicSupervisorId implements Serializable {
    private Long topic;
    private Long lecturer;
}
