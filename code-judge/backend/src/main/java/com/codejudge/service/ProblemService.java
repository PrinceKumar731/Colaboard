package com.codejudge.service;

import com.codejudge.dto.ProblemDto;
import com.codejudge.exception.ResourceNotFoundException;
import com.codejudge.model.Problem;
import com.codejudge.model.TestCase;
import com.codejudge.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final ProblemRepository problemRepository;

    @Transactional
    public ProblemDto createProblem(ProblemDto dto) {
        Problem problem = Problem.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .timeLimitMs(dto.getTimeLimitMs())
                .memoryLimitMb(dto.getMemoryLimitMb())
                .difficulty(dto.getDifficulty())
                .testCases(new ArrayList<>())
                .build();

        if (dto.getTestCases() != null) {
            for (int i = 0; i < dto.getTestCases().size(); i++) {
                ProblemDto.TestCaseDto tc = dto.getTestCases().get(i);
                problem.getTestCases().add(TestCase.builder()
                        .problem(problem)
                        .input(tc.getInput())
                        .expectedOutput(tc.getExpectedOutput())
                        .sample(tc.isSample())
                        .orderIndex(i)
                        .build());
            }
        }

        return toDto(problemRepository.save(problem));
    }

    @Transactional(readOnly = true)
    public ProblemDto getProblem(String id) {
        return problemRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<ProblemDto> listProblems(String difficulty, Pageable pageable) {
        Page<Problem> page = difficulty != null
                ? problemRepository.findByDifficulty(difficulty, pageable)
                : problemRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    private ProblemDto toDto(Problem p) {
        List<ProblemDto.TestCaseDto> sampleCases = p.getTestCases() == null ? List.of() :
                p.getTestCases().stream()
                        .filter(TestCase::isSample)
                        .map(tc -> new ProblemDto.TestCaseDto(tc.getInput(), tc.getExpectedOutput(), true))
                        .toList();

        return ProblemDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .timeLimitMs(p.getTimeLimitMs())
                .memoryLimitMb(p.getMemoryLimitMb())
                .difficulty(p.getDifficulty())
                .testCases(sampleCases)
                .build();
    }
}
