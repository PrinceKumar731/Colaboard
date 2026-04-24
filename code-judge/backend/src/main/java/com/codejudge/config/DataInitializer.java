package com.codejudge.config;

import com.codejudge.model.Problem;
import com.codejudge.model.TestCase;
import com.codejudge.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProblemRepository problemRepository;

    @Override
    public void run(String... args) {
        if (problemRepository.count() > 0) return;

        seedProblems();
        log.info("Seeded {} sample problems", problemRepository.count());
    }

    private void seedProblems() {
        // Problem 1 — Hello World
        Problem hello = Problem.builder()
                .title("Hello World")
                .description("Print 'Hello, World!' to stdout.")
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .difficulty("EASY")
                .testCases(new ArrayList<>())
                .build();
        addTestCase(hello, "", "Hello, World!", true, 0);
        addTestCase(hello, "", "Hello, World!", false, 1);
        problemRepository.save(hello);

        // Problem 2 — Sum of Two Numbers
        Problem sumTwo = Problem.builder()
                .title("Sum of Two Numbers")
                .description("""
                        Read two integers A and B from stdin (space-separated).
                        Print their sum on a single line.

                        Constraints: -10^9 <= A, B <= 10^9
                        """)
                .timeLimitMs(1000)
                .memoryLimitMb(256)
                .difficulty("EASY")
                .testCases(new ArrayList<>())
                .build();
        addTestCase(sumTwo, "3 5", "8", true, 0);
        addTestCase(sumTwo, "-1 1", "0", false, 1);
        addTestCase(sumTwo, "1000000000 1000000000", "2000000000", false, 2);
        problemRepository.save(sumTwo);

        // Problem 3 — FizzBuzz
        Problem fizzbuzz = Problem.builder()
                .title("FizzBuzz")
                .description("""
                        Read an integer N. For each number from 1 to N:
                        - Print "FizzBuzz" if divisible by both 3 and 5
                        - Print "Fizz" if divisible by 3
                        - Print "Buzz" if divisible by 5
                        - Otherwise print the number

                        Constraints: 1 <= N <= 100
                        """)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .difficulty("EASY")
                .testCases(new ArrayList<>())
                .build();
        addTestCase(fizzbuzz, "5", "1\n2\nFizz\n4\nBuzz", true, 0);
        addTestCase(fizzbuzz, "15", "1\n2\nFizz\n4\nBuzz\nFizz\n7\n8\nFizz\nBuzz\n11\nFizz\n13\n14\nFizzBuzz", false, 1);
        problemRepository.save(fizzbuzz);

        // Problem 4 — Reverse a String
        Problem reverseStr = Problem.builder()
                .title("Reverse a String")
                .description("""
                        Read a single line string S and print it reversed.

                        Constraints: 1 <= |S| <= 100,000
                        """)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .difficulty("EASY")
                .testCases(new ArrayList<>())
                .build();
        addTestCase(reverseStr, "hello", "olleh", true, 0);
        addTestCase(reverseStr, "abcdef", "fedcba", false, 1);
        addTestCase(reverseStr, "a", "a", false, 2);
        problemRepository.save(reverseStr);

        // Problem 5 — Fibonacci
        Problem fib = Problem.builder()
                .title("Nth Fibonacci Number")
                .description("""
                        Read an integer N and print the Nth Fibonacci number (0-indexed: F(0)=0, F(1)=1).

                        Constraints: 0 <= N <= 50
                        """)
                .timeLimitMs(2000)
                .memoryLimitMb(256)
                .difficulty("MEDIUM")
                .testCases(new ArrayList<>())
                .build();
        addTestCase(fib, "0", "0", true, 0);
        addTestCase(fib, "1", "1", true, 1);
        addTestCase(fib, "10", "55", false, 2);
        addTestCase(fib, "50", "12586269025", false, 3);
        problemRepository.save(fib);
    }

    private void addTestCase(Problem problem, String input, String expectedOutput,
                              boolean sample, int order) {
        problem.getTestCases().add(TestCase.builder()
                .problem(problem)
                .input(input)
                .expectedOutput(expectedOutput)
                .sample(sample)
                .orderIndex(order)
                .build());
    }
}
