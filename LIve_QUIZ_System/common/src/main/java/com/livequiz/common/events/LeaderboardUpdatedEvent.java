package com.livequiz.common.events;

import java.time.Instant;
import java.util.List;

public record LeaderboardUpdatedEvent(
        String sessionId,
        List<Entry> top,
        Instant updatedAt
) {
    public record Entry(String playerId, String displayName, double score, int rank) {}
}

