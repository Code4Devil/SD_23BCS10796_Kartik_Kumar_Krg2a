package com.livequiz.ws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * STOMP-over-WebSocket topology.
 *
 *   Client  -->  /app/answer            (sends answers)
 *   Client  -->  /app/behavior          (sends tab-switch / blur events)
 *   Client  <--  /topic/session/{id}/question
 *   Client  <--  /topic/session/{id}/leaderboard
 *   Client  <--  /topic/session/{id}/cheat-alert
 *
 * An in-memory simple broker is fine here because true fan-out between nodes
 * is handled by Redis Pub/Sub (see RedisListenerConfig). If scaling needs to
 * grow beyond 4–5 nodes, swap to RabbitMQ / ActiveMQ STOMP relay.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS fallback for browsers behind strict proxies.
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}

