package com.livequiz.ws.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livequiz.common.events.LeaderboardUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Receives LEADERBOARD_UPDATED JSON off Redis Pub/Sub and re-broadcasts it
 * to the local STOMP topic that players of that session are subscribed to.
 * This is the "fan-in from Redis, fan-out to local clients" half of our
 * pub/sub scaling strategy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardSubscriber implements MessageListener {

    private final ObjectMapper mapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            LeaderboardUpdatedEvent evt = mapper.readValue(message.getBody(), LeaderboardUpdatedEvent.class);
            messagingTemplate.convertAndSend(
                    "/topic/session/" + evt.sessionId() + "/leaderboard", evt);
        } catch (Exception ex) {
            log.error("Failed to decode leaderboard update", ex);
        }
    }
}

