package com.codejudge.repository;

import com.codejudge.model.Submission;
import com.codejudge.model.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {
    Page<Submission> findByUserId(String userId, Pageable pageable);
    Page<Submission> findByProblemId(String problemId, Pageable pageable);
    long countByStatus(SubmissionStatus status);
}
