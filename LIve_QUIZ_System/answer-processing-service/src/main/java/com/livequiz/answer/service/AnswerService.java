package com.livequiz.answer.service;

import com.livequiz.answer.model.AnswerLog;
import com.livequiz.answer.repository.AnswerLogRepository;
import com.livequiz.common.KafkaTopics;
import com.livequiz.common.dto.AnswerSubmissionDto;
import com.livequiz.common.events.AnswerSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Validates answers and emits ANSWER_SUBMITTED. Two safety nets:
 *   1. De-duplication: we SET-NX a Redis key `answered:{q}:{p}` so one player
 *      can only count once per question regardless of retries or duplicate
 *      WS deliveries. TTL matches the question window.
 *   2. Server-authoritative timing: response time is (now - question.publishedAt),
 *      not a client-supplied number — otherwise cheaters could claim 0ms.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private final QuestionCache questionCache;
    private final AnswerLogRepository logs;
    private final KafkaTemplate<String, Object> kafka;
    private final StringRedisTemplate redis;

    public AnswerSubmittedEvent submit(AnswerSubmissionDto dto) {
        Integer correctIdx = questionCache.correctOption(dto.questionId())
                .orElseThrow(() -> new IllegalStateException("Question is not live or has expired"));
        Instant publishedAt = questionCache.publishedAt(dto.questionId())
                .orElseThrow(() -> new IllegalStateException("Question publish time missing"));

        String dedupKey = "answered:" + dto.questionId() + ":" + dto.playerId();
        Boolean firstAttempt = redis.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(firstAttempt)) {
            throw new IllegalStateException("Answer already submitted for this question");
        }

        Instant now = Instant.now();
        long responseMs = Duration.between(publishedAt, now).toMillis();
        boolean correct = dto.selectedOptionIndex() == correctIdx;

        AnswerLog logEntry = AnswerLog.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(dto.sessionId()).playerId(dto.playerId())
                .questionId(dto.questionId()).selectedOptionIndex(dto.selectedOptionIndex())
                .correct(correct).responseTimeMs(responseMs).submittedAt(now)
                .build();
        logs.save(logEntry);

        AnswerSubmittedEvent evt = new AnswerSubmittedEvent(
                dto.sessionId(), dto.playerId(), dto.questionId(),
                dto.selectedOptionIndex(), correct, responseMs, now);
        kafka.send(KafkaTopics.ANSWER_SUBMITTED, dto.sessionId(), evt);
        log.debug("Answer {} -> correct={} in {}ms", dto.playerId(), correct, responseMs);
        return evt;
    }
}

