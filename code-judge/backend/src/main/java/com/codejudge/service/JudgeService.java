package com.codejudge.service;

import com.codejudge.model.ExecutionResult;
import com.codejudge.model.TestCase;
import com.codejudge.model.enums.Verdict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JudgeService {

    /**
     * Compares actual output against expected.
     * Normalizes trailing whitespace per line and trailing newlines — matches
     * the behavior of most competitive programming judges.
     */
    public Verdict judge(ExecutionResult result, TestCase testCase) {
        if (result.getVerdict() != Verdict.ACCEPTED) {
            return result.getVerdict();
        }

        String actual = normalize(result.getStdout());
        String expected = normalize(testCase.getExpectedOutput());

        if (actual.equals(expected)) {
            return Verdict.ACCEPTED;
        }

        log.debug("Wrong answer on test case {}: expected='{}', actual='{}'",
                testCase.getId(), expected, actual);
        return Verdict.WRONG_ANSWER;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.lines()
                .map(String::stripTrailing)
                .reduce("", (acc, line) -> acc.isEmpty() ? line : acc + "\n" + line)
                .strip();
    }
}
