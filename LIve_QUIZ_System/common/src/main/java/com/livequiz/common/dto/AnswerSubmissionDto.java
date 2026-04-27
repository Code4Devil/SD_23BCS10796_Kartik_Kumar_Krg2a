package com.livequiz.common.dto;

/**
 * DTO the client sends over WebSocket or REST when submitting an answer.
 * The server recomputes response time against the canonical `publishedAt`
 * timestamp of the question (we never trust a client-supplied latency).
 */
public record AnswerSubmissionDto(
        String sessionId,
        String playerId,
        String questionId,
        int selectedOptionIndex
) {}

