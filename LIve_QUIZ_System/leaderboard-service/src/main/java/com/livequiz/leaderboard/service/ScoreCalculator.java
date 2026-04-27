package com.livequiz.leaderboard.service;

import org.springframework.stereotype.Component;

/**
 * Flat-rate scoring:
 *   - A correct answer is worth POINTS_PER_CORRECT.
 *   - A wrong answer is 0.
 *
 * We keep this pure / stateless / unit-testable; scoring policy is the one
 * area product-managers love to tweak, so we isolate it behind one bean.
 */
@Component
public class ScoreCalculator {

    public static final int POINTS_PER_CORRECT = 5;

    public int score(boolean correct, long responseTimeMs, int timeLimitSeconds) {
        return correct ? POINTS_PER_CORRECT : 0;
    }
}

