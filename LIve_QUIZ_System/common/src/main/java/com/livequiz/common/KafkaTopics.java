package com.livequiz.common;

/**
 * Canonical list of Kafka topic names used across the platform.
 * Centralising these prevents typos in topic names from silently creating
 * orphan topics (Kafka auto-creates topics by default), which is one of the
 * most common operational bugs in event-driven architectures.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    /** Emitted by the Orchestrator when a host starts a quiz session. */
    public static final String QUIZ_STARTED = "quiz.started";

    /** Emitted by the Orchestrator for every question it publishes. */
    public static final String QUESTION_PUBLISHED = "quiz.question.published";

    /** Emitted by the Orchestrator when the quiz is over. */
    public static final String QUIZ_ENDED = "quiz.ended";

    /** Emitted by Answer Processing once an answer has been validated. */
    public static final String ANSWER_SUBMITTED = "quiz.answer.submitted";

    /** Emitted by Leaderboard Service after score recomputation. */
    public static final String LEADERBOARD_UPDATED = "quiz.leaderboard.updated";

    /** Emitted by Anti-Cheat when a suspicious pattern is detected. */
    public static final String CHEATING_DETECTED = "quiz.cheating.detected";

    /** Raw client-side behaviour signals ingested from the WebSocket Gateway. */
    public static final String BEHAVIOR_EVENTS = "quiz.behavior.events";
}

