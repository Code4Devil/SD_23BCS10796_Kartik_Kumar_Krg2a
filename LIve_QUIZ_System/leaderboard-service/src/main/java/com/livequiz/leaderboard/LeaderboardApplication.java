package com.livequiz.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Leaderboard Service — consumes ANSWER_SUBMITTED events, updates a Redis
 * Sorted Set per session (ZINCRBY), then publishes LEADERBOARD_UPDATED on a
 * Redis Pub/Sub channel that every WebSocket Gateway instance subscribes to.
 * Pub/Sub gives us fan-out without any service-to-service HTTP calls.
 */
@SpringBootApplication
public class LeaderboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaderboardApplication.class, args);
    }
}

