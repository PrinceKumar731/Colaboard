package com.codejudge.model;

import com.codejudge.model.enums.Language;
import com.codejudge.model.enums.SubmissionStatus;
import com.codejudge.model.enums.Verdict;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 10000)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Verdict verdict = Verdict.PENDING;

    private Long executionTimeMs;
    private Long memoryUsedKb;

    @Column(length = 5000)
    private String stdout;

    @Column(length = 5000)
    private String stderr;

    @Column(length = 2000)
    private String errorMessage;

    private String userId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
