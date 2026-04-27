package com.livequiz.ws.bridge;

import com.livequiz.common.KafkaTopics;
import com.livequiz.common.events.QuizEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumes Kafka QUIZ_ENDED and pushes it to session subscribers so clients
 * can transition to the result screen deterministically, without relying on a
 * client-side "no-question-arrived-in-N-seconds" timeout.
 */
@Component
@RequiredArgsConstructor
public class QuizEndedBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = KafkaTopics.QUIZ_ENDED,
                   containerFactory = "quizEndedListenerFactory")
    public void onQuizEnded(QuizEndedEvent e) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + e.sessionId() + "/ended", e);
    }
}

