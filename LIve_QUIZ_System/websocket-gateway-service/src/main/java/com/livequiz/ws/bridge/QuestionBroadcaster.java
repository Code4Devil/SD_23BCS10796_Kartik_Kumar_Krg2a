package com.livequiz.ws.bridge;

import com.livequiz.common.KafkaTopics;
import com.livequiz.common.events.QuestionPublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes Kafka QUESTION_PUBLISHED and pushes a client-safe payload over
 * STOMP. Crucially we STRIP the `correctOptionIndex` — that stays server-side
 * only (in the Redis cache of the Answer Processing service). Sending it
 * to the client would defeat the entire quiz.
 */
@Component
@RequiredArgsConstructor
public class QuestionBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = KafkaTopics.QUESTION_PUBLISHED,
                   containerFactory = "questionListenerFactory")
    public void onQuestion(QuestionPublishedEvent e) {
        Map<String, Object> safePayload = Map.of(
                "questionId", e.questionId(),
                "questionIndex", e.questionIndex(),
                "text", e.text(),
                "options", e.options(),
                "timeLimitSeconds", e.timeLimitSeconds(),
                // Clients compute their local countdown relative to this
                // server timestamp, giving us cross-client timer sync.
                "publishedAt", e.publishedAt().toString()
        );
        messagingTemplate.convertAndSend(
                "/topic/session/" + e.sessionId() + "/question", safePayload);
    }
}

