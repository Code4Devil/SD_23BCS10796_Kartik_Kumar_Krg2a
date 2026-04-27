package com.livequiz.session.service;

import com.livequiz.session.model.PlayerSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Redis key layout:
 *   session:{sessionId}:player:{playerId}  -> PlayerSession JSON (TTL 30 min)
 *   session:{sessionId}:roster             -> Set<playerId>       (TTL 30 min)
 *
 * The TTL acts as a cheap garbage collector: if nobody refreshes the roster
 * the key simply expires. The roster Set lets the WebSocket Gateway answer
 * "who is in this room?" with an O(N) SMEMBERS call.
 */
@Service
public class PlayerSessionService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration DISCONNECT_GRACE = Duration.ofSeconds(15);

    private final RedisTemplate<String, PlayerSession> sessionRedis;
    private final RedisTemplate<String, String> stringRedis;

    public PlayerSessionService(
            @Qualifier("sessionTemplate") RedisTemplate<String, PlayerSession> sessionRedis,
            @Qualifier("stringTemplate") RedisTemplate<String, String> stringRedis) {
        this.sessionRedis = sessionRedis;
        this.stringRedis = stringRedis;
    }

    public PlayerSession join(String sessionId, String playerId, String displayName) {
        String key = key(sessionId, playerId);
        PlayerSession existing = sessionRedis.opsForValue().get(key);
        PlayerSession session = existing != null ? existing : PlayerSession.builder()
                .sessionId(sessionId).playerId(playerId)
                .displayName(displayName).joinedAt(Instant.now())
                .build();
        session.setConnected(true);
        session.setLastHeartbeatAt(Instant.now());
        sessionRedis.opsForValue().set(key, session, SESSION_TTL);
        stringRedis.opsForSet().add(rosterKey(sessionId), playerId);
        stringRedis.expire(rosterKey(sessionId), SESSION_TTL);
        return session;
    }

    public void leave(String sessionId, String playerId) {
        PlayerSession s = sessionRedis.opsForValue().get(key(sessionId, playerId));
        if (s != null) {
            s.setConnected(false);
            sessionRedis.opsForValue().set(key(sessionId, playerId), s, SESSION_TTL);
        }
    }

    public Optional<PlayerSession> heartbeat(String sessionId, String playerId) {
        PlayerSession s = sessionRedis.opsForValue().get(key(sessionId, playerId));
        if (s == null) return Optional.empty();
        s.setLastHeartbeatAt(Instant.now());
        s.setConnected(true);
        sessionRedis.opsForValue().set(key(sessionId, playerId), s, SESSION_TTL);
        return Optional.of(s);
    }

    public Set<String> roster(String sessionId) {
        Set<String> members = stringRedis.opsForSet().members(rosterKey(sessionId));
        return members == null ? Set.of() : members;
    }

    public boolean isAlive(PlayerSession s) {
        return s.getLastHeartbeatAt() != null &&
                Duration.between(s.getLastHeartbeatAt(), Instant.now()).compareTo(DISCONNECT_GRACE) < 0;
    }

    private String key(String sessionId, String playerId) {
        return "session:" + sessionId + ":player:" + playerId;
    }

    private String rosterKey(String sessionId) {
        return "session:" + sessionId + ":roster";
    }
}

