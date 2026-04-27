package com.livequiz.leaderboard.service;

import com.livequiz.common.KafkaTopics;
import com.livequiz.common.events.AnswerSubmittedEvent;
import com.livequiz.common.events.LeaderboardUpdatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Core leaderboard mechanics.
 *
 * Redis sorted set key:   quiz:{sessionId}:leaderboard
 *   - Score = cumulative points.
 *   - Members are playerIds; display names are stored in a companion hash
 *     quiz:{sessionId}:names so the sorted set stays small and fast.
 *
 * Why ZSET: built-in O(log N) ZINCRBY, O(log N + M) ZREVRANGE-with-scores for
 * the top-K query, and naturally sorted output — this is the canonical Redis
 * leaderboard pattern and scales to millions of entries.
 *
 * Why Pub/Sub for fan-out: WebSocket Gateway nodes are stateless and
 * horizontally scaled; Pub/Sub broadcasts the update to all of them without
 * a service-registry lookup. The trade-off is "at-most-once" delivery — if a
 * WS node is momentarily disconnected from Redis it misses a beat, but the
 * next LEADERBOARD_UPDATED will resync it (eventually consistent).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final StringRedisTemplate redis;
    private final ScoreCalculator scoreCalculator;
    private final ObjectMapper mapper;

    @Value("${livequiz.leaderboard.top-k:10}") private int topK;
    @Value("${livequiz.leaderboard.pubsub-channel:leaderboard.updates}") private String channel;

    @KafkaListener(topics = KafkaTopics.ANSWER_SUBMITTED, containerFactory = "listenerFactory")
    public void onAnswer(AnswerSubmittedEvent e) {
        // NOTE: timeLimit isn't in the event (to keep it thin); we default
        // to 30s here. A richer version would carry it through or look it up
        // from the same Redis question-cache the AnswerService writes to.
        int points = scoreCalculator.score(e.correct(), e.responseTimeMs(), 30);
        if (points > 0) {
            redis.opsForZSet().incrementScore(lbKey(e.sessionId()), e.playerId(), points);
            redis.expire(lbKey(e.sessionId()), Duration.ofHours(2));
        }
        publishTop(e.sessionId());
    }

    public void registerDisplayName(String sessionId, String playerId, String displayName) {
        redis.opsForHash().put(nameKey(sessionId), playerId, displayName);
        redis.expire(nameKey(sessionId), Duration.ofHours(2));
        // Seed the zset with a zero score the first time we see this player so
        // the leaderboard shows every joined player from the lobby onwards.
        // addIfAbsent is ZADD NX — existing (non-zero) scores are preserved.
        redis.opsForZSet().addIfAbsent(lbKey(sessionId), playerId, 0.0);
        redis.expire(lbKey(sessionId), Duration.ofHours(2));
        publishTop(sessionId);
    }

    public List<LeaderboardUpdatedEvent.Entry> top(String sessionId) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                redis.opsForZSet().reverseRangeWithScores(lbKey(sessionId), 0, topK - 1);
        if (tuples == null) return List.of();
        List<LeaderboardUpdatedEvent.Entry> list = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            String name = (String) redis.opsForHash().get(nameKey(sessionId), t.getValue());
            list.add(new LeaderboardUpdatedEvent.Entry(
                    t.getValue(),
                    name != null ? name : t.getValue(),
                    t.getScore() == null ? 0 : t.getScore(),
                    rank++));
        }
        return list;
    }

    private void publishTop(String sessionId) {
        LeaderboardUpdatedEvent evt = new LeaderboardUpdatedEvent(sessionId, top(sessionId), Instant.now());
        try {
            redis.convertAndSend(channel, mapper.writeValueAsString(evt));
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialise leaderboard update", ex);
        }
    }

    private String lbKey(String s)   { return "quiz:" + s + ":leaderboard"; }
    private String nameKey(String s) { return "quiz:" + s + ":names"; }
}

