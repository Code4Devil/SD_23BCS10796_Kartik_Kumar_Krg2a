package com.livequiz.answer.service;

import com.livequiz.common.KafkaTopics;
import com.livequiz.common.events.QuestionPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Hot cache for the currently-active question in each session.
 *
 * Key format:
 *   question:{questionId}:correct   -> "3"                 (TTL = timeLimit + 1min)
 *   question:{questionId}:published -> ISO-8601 timestamp  (TTL = timeLimit + 1min)
 *
 * We deliberately cache only the minimum needed for validation — never the
 * question text or options (that's the WebSocket Gateway's broadcast concern).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionCache {

    private final StringRedisTemplate redis;

    @KafkaListener(topics = KafkaTopics.QUESTION_PUBLISHED,
                   containerFactory = "questionListenerFactory")
    public void onQuestionPublished(QuestionPublishedEvent e) {
        Duration ttl = Duration.ofSeconds(e.timeLimitSeconds() + 60L);
        redis.opsForValue().set(correctKey(e.questionId()), String.valueOf(e.correctOptionIndex()), ttl);
        redis.opsForValue().set(publishedKey(e.questionId()), e.publishedAt().toString(), ttl);
        log.debug("Cached Q {} for session {}", e.questionId(), e.sessionId());
    }

    public Optional<Integer> correctOption(String questionId) {
        String v = redis.opsForValue().get(correctKey(questionId));
        return v == null ? Optional.empty() : Optional.of(Integer.parseInt(v));
    }

    public Optional<Instant> publishedAt(String questionId) {
        String v = redis.opsForValue().get(publishedKey(questionId));
        return v == null ? Optional.empty() : Optional.of(Instant.parse(v));
    }

    private String correctKey(String qid)   { return "question:" + qid + ":correct"; }
    private String publishedKey(String qid) { return "question:" + qid + ":published"; }
}

