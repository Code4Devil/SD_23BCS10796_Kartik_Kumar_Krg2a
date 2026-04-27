package com.livequiz.common.events;

import java.time.Instant;

/**
 * Produced by the Answer Processing service after it has validated a raw
 * answer submission from the client. Carrying both `correct` and
 * `responseTimeMs` in the same event lets the Leaderboard service compute
 * scores without needing a second round-trip to any database.
 */
public record AnswerSubmittedEvent(
        String sessionId,
        String playerId,
        String questionId,
        int selectedOptionIndex,
        boolean correct,
        long responseTimeMs,
        Instant submittedAt
) {}

