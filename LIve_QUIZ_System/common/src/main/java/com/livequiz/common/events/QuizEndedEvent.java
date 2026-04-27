package com.livequiz.common.events;

import java.time.Instant;

public record QuizEndedEvent(
        String sessionId,
        String quizId,
        Instant endedAt
) {}

