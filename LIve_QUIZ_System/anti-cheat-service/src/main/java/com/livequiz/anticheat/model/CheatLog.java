package com.livequiz.anticheat.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Every suspicious signal we observe is appended here. Compound index on
 * (sessionId, playerId) keeps the per-player aggregation query cheap when we
 * need to decide whether the running total has tripped the flag threshold.
 */
@Document(collection = "cheat_logs")
@CompoundIndex(name = "session_player_idx", def = "{'sessionId': 1, 'playerId': 1, 'occurredAt': -1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CheatLog {
    @Id private String id;
    private String sessionId;
    private String playerId;
    private String type;          // TAB_HIDDEN / WINDOW_BLUR / RAPID_ANSWER / MULTI_SESSION
    private int severityScore;    // running weight contributed by this event
    private Map<String, Object> metadata;
    private Instant occurredAt;
}

