package com.codejudge.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(nullable = false, length = 5000)
    private String input;

    @Column(nullable = false, length = 5000)
    private String expectedOutput;

    @Column(name = "is_sample")
    @Builder.Default
    private boolean sample = false;

    @Column(name = "order_index")
    @Builder.Default
    private int orderIndex = 0;
}
