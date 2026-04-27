package com.livequiz.session.web;

import com.livequiz.common.dto.JoinSessionDto;
import com.livequiz.session.model.PlayerSession;
import com.livequiz.session.service.PlayerSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final PlayerSessionService service;

    /** Join (or transparently re-join) a quiz room. Idempotent. */
    @PostMapping("/join")
    public PlayerSession join(@RequestBody JoinSessionDto dto) {
        return service.join(dto.sessionId(), dto.playerId(), dto.displayName());
    }

    /**
     * Heartbeat keeps the session "alive". Clients should ping every 5-10s;
     * the gateway will also auto-heartbeat on every inbound WS frame.
     */
    @PostMapping("/{sessionId}/heartbeat")
    public ResponseEntity<PlayerSession> heartbeat(@PathVariable String sessionId,
                                                   @RequestHeader("X-Player-Id") String playerId) {
        return service.heartbeat(sessionId, playerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{sessionId}/leave")
    public void leave(@PathVariable String sessionId,
                      @RequestHeader("X-Player-Id") String playerId) {
        service.leave(sessionId, playerId);
    }

    @GetMapping("/{sessionId}/roster")
    public Set<String> roster(@PathVariable String sessionId) {
        return service.roster(sessionId);
    }
}

