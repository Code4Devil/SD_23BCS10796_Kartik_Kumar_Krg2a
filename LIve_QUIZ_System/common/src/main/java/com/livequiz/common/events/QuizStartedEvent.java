package com.livequiz.common.events;

import java.time.Instant;

/**
 * Fired once per quiz run, immediately after the host presses "Start".
 * Carries only the identifiers + a server-side timestamp — all mutable state
 * (current question index, remaining time) lives in the orchestrator and is
 * synced via subsequent QuestionPublishedEvents.
 */
public record QuizStartedEvent(
        String sessionId,
        String quizId,
        String hostId,
        Instant startedAt
) {}

