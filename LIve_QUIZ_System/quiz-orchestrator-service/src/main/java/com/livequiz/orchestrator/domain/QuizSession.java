package com.livequiz.orchestrator.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One execution of a quiz. Carries enough state to resume if this service
 * restarts mid-quiz (currentQuestionIndex + currentQuestionPublishedAt).
 * Durable in PostgreSQL so a pod restart does not lose quiz progress.
 */
@Entity
@Table(name = "quiz_session")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuizSession {

    public enum Status { CREATED, RUNNING, ENDED }

    @Id
    private String id;

    @Column(name = "quiz_id", nullable = false)
    private String quizId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int currentQuestionIndex;

    private Instant currentQuestionPublishedAt;

    private Instant startedAt;
    private Instant endedAt;
}

