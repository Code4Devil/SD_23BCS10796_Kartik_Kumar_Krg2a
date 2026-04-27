package com.livequiz.common.events;

import java.time.Instant;

public record CheatingDetectedEvent(
        String sessionId,
        String playerId,
        String reason,
        Severity severity,
        Instant detectedAt
) {
    public enum Severity { WARN, FLAG, DISQUALIFY }
}

