package com.quanlydetai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_code", unique = true, nullable = false, length = 20)
    private String deptCode;

    @Column(name = "dept_name", nullable = false, length = 150)
    private String deptName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_of_dept_id")
    private User headLecturer;

    @Transient
    private LocalDateTime createdAt;
}
