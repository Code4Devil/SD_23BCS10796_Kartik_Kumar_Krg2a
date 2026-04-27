package com.livequiz.leaderboard.web;

import com.livequiz.common.events.LeaderboardUpdatedEvent;
import com.livequiz.leaderboard.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService service;

    /** Snapshot fetch — used for initial page load before the WS subscription. */
    @GetMapping("/{sessionId}/top")
    public List<LeaderboardUpdatedEvent.Entry> top(@PathVariable String sessionId) {
        return service.top(sessionId);
    }

    /**
     * Display-name registration. Exposed as a plain endpoint so the Player
     * Session service (or any admin tool) can tell the leaderboard service
     * how to render each player — avoiding a cross-service lookup at read time.
     */
    @PostMapping("/{sessionId}/names")
    public void register(@PathVariable String sessionId,
                         @RequestParam String playerId,
                         @RequestParam String displayName) {
        service.registerDisplayName(sessionId, playerId, displayName);
    }
}

