package com.livequiz.session.model;

import lombok.*;

import java.time.Instant;

/**
 * In-memory player membership record. `lastHeartbeatAt` is updated whenever
 * the client pings (or a WebSocket frame arrives); if the heartbeat is stale
 * for longer than a grace window, the session is considered disconnected.
 * A reconnect within the TTL restores the same record — that's how we keep
 * scores consistent across transient network blips.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlayerSession {
    private String sessionId;
    private String playerId;
    private String displayName;
    private Instant joinedAt;
    private Instant lastHeartbeatAt;
    private boolean connected;
}

