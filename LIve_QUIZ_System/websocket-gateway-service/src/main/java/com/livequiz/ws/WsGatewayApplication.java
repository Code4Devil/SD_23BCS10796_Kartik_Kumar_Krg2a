package com.livequiz.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WebSocket Gateway Service — the only service that holds long-lived client
 * connections. All other services are stateless request/response or pure
 * Kafka consumers.
 *
 * Scaling model:
 *   - N gateway nodes behind a sticky-session-less load balancer.
 *   - Each node subscribes to Redis Pub/Sub channels.
 *   - Kafka consumers on each node DO NOT broadcast directly; they publish
 *     to Redis Pub/Sub so every node sees every message and can fan out to
 *     its locally-connected clients.
 *   - With 10K connections/node and 4 nodes we can sustain the 10K-user
 *     target quoted in the NFRs, with plenty of headroom.
 */
@SpringBootApplication
public class WsGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(WsGatewayApplication.class, args);
    }
}

