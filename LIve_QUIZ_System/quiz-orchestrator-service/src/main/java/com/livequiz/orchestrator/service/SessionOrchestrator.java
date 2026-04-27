package com.livequiz.orchestrator.service;

import com.livequiz.common.KafkaTopics;
import com.livequiz.common.events.*;
import com.livequiz.orchestrator.domain.*;
import com.livequiz.orchestrator.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The brain of the live-quiz pipeline.
 *
 * A scheduled tick (runs every second) advances RUNNING sessions through
 * their question list: once a question's time limit is up, the next question
 * is published as a Kafka event. The scheduler is idempotent w.r.t. restarts
 * because session progress (currentQuestionIndex + publishedAt) is persisted
 * in PostgreSQL.
 *
 * Trade-off note: a 1-second tick is coarse but good enough for quiz timers
 * that are typically 10–60 seconds. If sub-second timing is ever needed we'd
 * switch to a per-session delayed task scheduled with ScheduledExecutorService
 * or Kafka's timer-wheel via Kafka Streams.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionOrchestrator {

    private final QuizSessionRepository sessionRepository;
    private final QuizService quizService;
    private final KafkaTemplate<String, Object> kafka;

    @Transactional
    public QuizSession startSession(String quizId, String hostId) {
        // Create the room in CREATED (lobby) state. The first question is NOT
        // published until the host explicitly calls beginSession — this gives
        // players time to join before the timer starts ticking.
        QuizSession session = QuizSession.builder()
                .id(UUID.randomUUID().toString())
                .quizId(quizId)
                .status(QuizSession.Status.CREATED)
                .currentQuestionIndex(-1)
                .startedAt(Instant.now())
                .build();
        sessionRepository.save(session);

        kafka.send(KafkaTopics.QUIZ_STARTED, session.getId(),
                new QuizStartedEvent(session.getId(), quizId, hostId, session.getStartedAt()));
        return session;
    }

    @Transactional
    public QuizSession beginSession(String sessionId) {
        QuizSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionId));
        if (session.getStatus() != QuizSession.Status.CREATED) {
            throw new IllegalStateException(
                    "session " + sessionId + " cannot be begun from status " + session.getStatus());
        }
        session.setStatus(QuizSession.Status.RUNNING);
        sessionRepository.save(session);
        publishNextQuestion(session);
        return session;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void tick() {
        List<QuizSession> running = sessionRepository.findByStatus(QuizSession.Status.RUNNING);
        for (QuizSession s : running) {
            if (s.getCurrentQuestionPublishedAt() == null) continue;
            Quiz quiz = quizService.findById(s.getQuizId());
            Question current = quiz.getQuestions().get(s.getCurrentQuestionIndex());
            Duration elapsed = Duration.between(s.getCurrentQuestionPublishedAt(), Instant.now());
            if (elapsed.getSeconds() >= current.getTimeLimitSeconds()) {
                publishNextQuestion(s);
            }
        }
    }

    private void publishNextQuestion(QuizSession session) {
        Quiz quiz = quizService.findById(session.getQuizId());
        int next = session.getCurrentQuestionIndex() + 1;
        if (next >= quiz.getQuestions().size()) {
            endSession(session);
            return;
        }
        Question q = quiz.getQuestions().get(next);
        Instant now = Instant.now();
        session.setCurrentQuestionIndex(next);
        session.setCurrentQuestionPublishedAt(now);
        sessionRepository.save(session);

        QuestionPublishedEvent event = new QuestionPublishedEvent(
                session.getId(), q.getId(), next, q.getText(), q.getOptions(),
                q.getCorrectOptionIndex(), q.getTimeLimitSeconds(), now);
        kafka.send(KafkaTopics.QUESTION_PUBLISHED, session.getId(), event);
        log.info("Published Q{} for session {}", next, session.getId());
    }

    private void endSession(QuizSession session) {
        session.setStatus(QuizSession.Status.ENDED);
        session.setEndedAt(Instant.now());
        sessionRepository.save(session);
        kafka.send(KafkaTopics.QUIZ_ENDED, session.getId(),
                new QuizEndedEvent(session.getId(), session.getQuizId(), session.getEndedAt()));
        log.info("Ended session {}", session.getId());
    }
}

