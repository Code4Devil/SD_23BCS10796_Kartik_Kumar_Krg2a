package com.livequiz.answer.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Append-only audit record of every answer submission. Stored in MongoDB
 * because:
 *   - Write-heavy, read-light workload (perfect for Mongo's storage engine).
 *   - We frequently need to change what we log (new anti-cheat fields, etc.);
 *     schemaless storage makes that a non-event.
 *   - Analytics queries ("avg response time per question in session X") are
 *     fine with Mongo aggregations; we don't need relational joins here.
 */
@Document(collection = "answer_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnswerLog {
    @Id private String id;
    private String sessionId;
    private String playerId;
    private String questionId;
    private int selectedOptionIndex;
    private boolean correct;
    private long responseTimeMs;
    private Instant submittedAt;
}

