package com.livequiz.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Player Session Service — the membership registry for live quiz rooms.
 * Backing store is Redis only (no DB): sessions are ephemeral and need
 * sub-millisecond reads for the "who is currently in this room?" query.
 */
@SpringBootApplication
public class SessionApplication {
    public static void main(String[] args) {
        SpringApplication.run(SessionApplication.class, args);
    }
}

