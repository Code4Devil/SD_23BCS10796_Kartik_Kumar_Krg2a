package com.livequiz.ws.ws;

import com.livequiz.common.KafkaTopics;
import com.livequiz.common.dto.AnswerSubmissionDto;
import com.livequiz.common.events.BehaviorEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClient;

/**
 * STOMP inbound controller. The gateway doesn't do business logic itself —
 * it just translates WebSocket frames into calls to the domain services:
 *   - answers go to the Answer Processing service over REST (which also
 *     dedups and validates before producing ANSWER_SUBMITTED to Kafka),
 *   - behaviour events go directly onto Kafka for the Anti-Cheat service
 *     to consume (no synchronous RPC needed — fire-and-forget).
 *
 * This keeps the gateway stateless and cheap to scale horizontally.
 */
@Controller
@RequiredArgsConstructor
public class ClientMessageController {

    private final KafkaTemplate<String, Object> kafka;

    @Value("${livequiz.answer-service.url:http://localhost:8083}")
    private String answerServiceUrl;

    private RestClient rest;

    @PostConstruct
    void init() {
        this.rest = RestClient.builder().baseUrl(answerServiceUrl).build();
    }

    @MessageMapping("/answer")
    public void onAnswer(AnswerSubmissionDto dto) {
        rest.post().uri("/api/answer/submit").body(dto).retrieve().toBodilessEntity();
    }

    @MessageMapping("/behavior")
    public void onBehavior(BehaviorEvent evt) {
        kafka.send(KafkaTopics.BEHAVIOR_EVENTS, evt.sessionId(), evt);
    }
}

