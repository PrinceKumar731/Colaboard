package com.codejudge.repository;

import com.codejudge.model.Problem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, String> {
    Page<Problem> findByDifficulty(String difficulty, Pageable pageable);
}
