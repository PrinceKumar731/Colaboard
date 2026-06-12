package com.colaboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ColaboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(ColaboardApplication.class, args);
    }
}
