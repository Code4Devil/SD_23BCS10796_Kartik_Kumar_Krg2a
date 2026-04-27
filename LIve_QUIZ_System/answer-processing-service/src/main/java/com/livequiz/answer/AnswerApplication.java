package com.livequiz.answer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Answer Processing Service.
 *
 * Responsible for:
 *  1. Caching the "correct option" of each live question in Redis, fed by the
 *     QUESTION_PUBLISHED Kafka event. This turns answer validation into a
 *     single O(1) Redis GET instead of a DB lookup — essential for sub-100ms
 *     latency at 10K concurrent answers/sec.
 *  2. Receiving raw answer submissions (via REST from the WS Gateway),
 *     validating them against the cached correct answer, computing the
 *     server-authoritative response time, and emitting ANSWER_SUBMITTED.
 *  3. Writing an immutable audit log to MongoDB (schema-flexible, high
 *     write-throughput — fits the workload better than Postgres).
 */
@SpringBootApplication
public class AnswerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnswerApplication.class, args);
    }
}

