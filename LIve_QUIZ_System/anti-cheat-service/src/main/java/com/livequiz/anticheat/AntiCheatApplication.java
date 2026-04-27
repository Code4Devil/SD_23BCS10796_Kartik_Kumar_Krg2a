package com.livequiz.anticheat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Anti-Cheat Service.
 *
 * Two Kafka consumer groups:
 *   1. BEHAVIOR_EVENTS — client signals (tab hide, window blur, copy/paste).
 *   2. ANSWER_SUBMITTED — inspected for statistically-impossible response times.
 *
 * Detections are scored, persisted to MongoDB (detailed audit), and — if the
 * threshold is breached — re-emitted as CHEATING_DETECTED for the orchestrator
 * / leaderboard to act upon (warn, flag, disqualify).
 */
@SpringBootApplication
public class AntiCheatApplication {
    public static void main(String[] args) {
        SpringApplication.run(AntiCheatApplication.class, args);
    }
}

