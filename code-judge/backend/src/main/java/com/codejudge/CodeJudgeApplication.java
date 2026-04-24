package com.codejudge;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Code Judge Engine API",
        version = "1.0",
        description = "Secure multi-language code execution engine with Docker sandboxing"
))
public class CodeJudgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeJudgeApplication.class, args);
    }
}
