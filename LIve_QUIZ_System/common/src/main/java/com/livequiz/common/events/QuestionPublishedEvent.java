package com.livequiz.common.events;

import java.time.Instant;
import java.util.List;

/**
 * The orchestrator emits this once per question. Two kinds of consumers exist:
 *   - The WebSocket Gateway, which broadcasts the question (MINUS the correct
 *     answer) to every active player.
 *   - The Answer Processing service, which caches the correctAnswerIndex in
 *     Redis so it can validate answers with a single in-memory lookup.
 *
 * `publishedAt` is authoritative server time: clients compute their response
 * latency relative to this rather than their own clocks.
 */
public record QuestionPublishedEvent(
        String sessionId,
        String questionId,
        int questionIndex,
        String text,
        List<String> options,
        int correctOptionIndex,
        int timeLimitSeconds,
        Instant publishedAt
) {}

