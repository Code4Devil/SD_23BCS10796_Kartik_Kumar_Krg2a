package com.livequiz.anticheat.web;

import com.livequiz.anticheat.model.CheatLog;
import com.livequiz.anticheat.repository.CheatLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only admin endpoints for the host dashboard. Write path is entirely
 * event-driven (Kafka BEHAVIOR_EVENTS + ANSWER_SUBMITTED).
 */
@RestController
@RequestMapping("/api/anticheat")
@RequiredArgsConstructor
public class AntiCheatController {

    private final CheatLogRepository repo;

    @GetMapping("/{sessionId}/players/{playerId}/logs")
    public List<CheatLog> logs(@PathVariable String sessionId, @PathVariable String playerId) {
        return repo.findBySessionIdAndPlayerId(sessionId, playerId);
    }

    /**
     * Per-player aggregate counts for the host dashboard — one row per player
     * with event-type counts and a running severity total. Cheap because
     * CheatLog has a compound (sessionId, playerId) index and sessions are
     * scoped to minutes, not days.
     */
    @GetMapping("/{sessionId}/stats")
    public List<PlayerStats> stats(@PathVariable String sessionId) {
        Map<String, PlayerStats> byPlayer = new HashMap<>();
        for (CheatLog l : repo.findBySessionId(sessionId)) {
            PlayerStats s = byPlayer.computeIfAbsent(l.getPlayerId(),
                    pid -> new PlayerStats(pid, new HashMap<>(), 0));
            s.counts().merge(l.getType(), 1, Integer::sum);
            s = new PlayerStats(s.playerId(), s.counts(),
                    s.totalSeverity() + l.getSeverityScore());
            byPlayer.put(l.getPlayerId(), s);
        }
        return new ArrayList<>(byPlayer.values());
    }

    public record PlayerStats(String playerId, Map<String, Integer> counts, int totalSeverity) {}
}

