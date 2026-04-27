package com.livequiz.orchestrator.domain;

import jakarta.persistence.*;
import lombok.*;

/**
 * Final per-player score persisted when a quiz ends. Leaderboard Service is
 * the source of truth during the quiz (Redis sorted set); this table is the
 * durable, queryable archive written once at quiz-end.
 */
@Entity
@Table(name = "result", indexes = {
        @Index(name = "idx_result_session", columnList = "session_id"),
        @Index(name = "idx_result_player", columnList = "player_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Result {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    private double score;
    private int correctAnswers;
    private int rank;
}

