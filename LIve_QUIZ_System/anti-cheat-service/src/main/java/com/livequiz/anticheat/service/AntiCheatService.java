package com.livequiz.anticheat.service;

import com.livequiz.anticheat.model.CheatLog;
import com.livequiz.anticheat.repository.CheatLogRepository;
import com.livequiz.common.KafkaTopics;
import com.livequiz.common.events.AnswerSubmittedEvent;
import com.livequiz.common.events.BehaviorEvent;
import com.livequiz.common.events.CheatingDetectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Rule-based (not ML) anti-cheat detection. Deliberately simple and explainable
 * so a teacher reviewing an "incident report" can see exactly what triggered it.
 *
 * Thresholds are tuned for demo purposes:
 *   score >= 10  -> WARN (silent notice to the player)
 *   score >= 25  -> FLAG (visible to the host on the dashboard)
 *   score >= 50  -> DISQUALIFY (orchestrator zeroes score + freezes player)
 *
 * A production system would augment this with a streaming ML model
 * (e.g. Kafka Streams + TensorFlow-Lite) but the event contract stays the same.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiCheatService {

    private static final int THRESHOLD_WARN = 10;
    private static final int THRESHOLD_FLAG = 25;
    private static final int THRESHOLD_DISQUALIFY = 50;
    private static final long RAPID_ANSWER_FLOOR_MS = 400; // sub-400ms reads+answers are implausible

    private final CheatLogRepository repo;
    private final KafkaTemplate<String, Object> kafka;

    @KafkaListener(topics = KafkaTopics.BEHAVIOR_EVENTS, containerFactory = "behaviorListenerFactory")
    public void onBehavior(BehaviorEvent e) {
        int weight = switch (e.type()) {
            case TAB_HIDDEN, WINDOW_BLUR -> 5;
            case FULLSCREEN_EXIT -> 7;
            case COPY, PASTE -> 8;
            case MULTIPLE_SESSION_DETECTED -> 30;
            case TAB_VISIBLE, WINDOW_FOCUS -> 0;  // benign, but logged
        };
        if (weight == 0) return;
        logAndMaybeEmit(e.sessionId(), e.playerId(), e.type().name(), weight,
                Map.of("userAgent", e.userAgent() == null ? "" : e.userAgent(),
                       "ip", e.ipAddress() == null ? "" : e.ipAddress()));
    }

    @KafkaListener(topics = KafkaTopics.ANSWER_SUBMITTED, containerFactory = "answerListenerFactory")
    public void onAnswer(AnswerSubmittedEvent e) {
        if (e.responseTimeMs() >= RAPID_ANSWER_FLOOR_MS) return;
        logAndMaybeEmit(e.sessionId(), e.playerId(), "RAPID_ANSWER", 12,
                Map.of("responseMs", e.responseTimeMs(), "questionId", e.questionId()));
    }

    private void logAndMaybeEmit(String sessionId, String playerId, String type,
                                 int weight, Map<String, Object> meta) {
        repo.save(CheatLog.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId).playerId(playerId)
                .type(type).severityScore(weight)
                .metadata(meta).occurredAt(Instant.now())
                .build());

        int total = repo.findBySessionIdAndPlayerId(sessionId, playerId)
                .stream().mapToInt(CheatLog::getSeverityScore).sum();

        CheatingDetectedEvent.Severity severity = null;
        if (total >= THRESHOLD_DISQUALIFY) severity = CheatingDetectedEvent.Severity.DISQUALIFY;
        else if (total >= THRESHOLD_FLAG)  severity = CheatingDetectedEvent.Severity.FLAG;
        else if (total >= THRESHOLD_WARN)  severity = CheatingDetectedEvent.Severity.WARN;

        if (severity != null) {
            CheatingDetectedEvent evt = new CheatingDetectedEvent(
                    sessionId, playerId,
                    "Accumulated score " + total + " triggered by " + type,
                    severity, Instant.now());
            kafka.send(KafkaTopics.CHEATING_DETECTED, sessionId, evt);
            log.warn("Cheat detected: session={} player={} total={} severity={}",
                    sessionId, playerId, total, severity);
        }
    }
}

