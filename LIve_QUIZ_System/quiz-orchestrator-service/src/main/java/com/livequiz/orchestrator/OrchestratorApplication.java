package com.livequiz.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bootstraps the Quiz Orchestrator. Owns the canonical schedule of the quiz:
 * which question is currently live and when the next one should be published.
 * All other services react to the events this service emits.
 */
@SpringBootApplication
@EnableScheduling
public class OrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestratorApplication.class, args);
    }
}

