package com.codejudge.controller;

import com.codejudge.dto.ProblemDto;
import com.codejudge.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
@Tag(name = "Problems", description = "Manage coding problems and test cases")
public class ProblemController {

    private final ProblemService problemService;

    @PostMapping
    @Operation(summary = "Create a new problem with test cases")
    public ResponseEntity<ProblemDto> createProblem(@Valid @RequestBody ProblemDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(problemService.createProblem(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get problem by ID (sample test cases only)")
    public ResponseEntity<ProblemDto> getProblem(@PathVariable String id) {
        return ResponseEntity.ok(problemService.getProblem(id));
    }

    @GetMapping
    @Operation(summary = "List all problems, optionally filtered by difficulty")
    public ResponseEntity<Page<ProblemDto>> listProblems(
            @RequestParam(required = false) String difficulty,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(problemService.listProblems(difficulty, pageable));
    }
}
