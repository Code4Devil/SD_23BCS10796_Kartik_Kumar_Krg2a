package com.livequiz.common.events;

import java.time.Instant;

/**
 * Raw client-side behavioural signal (tab switch, window blur, focus return,
 * suspicious input). Kept deliberately thin: the Anti-Cheat service is the
 * only component that understands how to score these.
 */
public record BehaviorEvent(
        String sessionId,
        String playerId,
        BehaviorType type,
        String userAgent,
        String ipAddress,
        Instant occurredAt
) {
    public enum BehaviorType {
        TAB_HIDDEN,
        TAB_VISIBLE,
        WINDOW_BLUR,
        WINDOW_FOCUS,
        COPY,
        PASTE,
        FULLSCREEN_EXIT,
        MULTIPLE_SESSION_DETECTED
    }
}

