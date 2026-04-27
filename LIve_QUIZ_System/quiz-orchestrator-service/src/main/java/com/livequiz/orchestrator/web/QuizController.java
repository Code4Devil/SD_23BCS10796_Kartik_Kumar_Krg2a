package com.livequiz.orchestrator.web;

import com.livequiz.orchestrator.domain.Quiz;
import com.livequiz.orchestrator.domain.QuizSession;
import com.livequiz.orchestrator.service.QuizService;
import com.livequiz.orchestrator.service.SessionOrchestrator;
import com.livequiz.orchestrator.web.dto.CreateQuizRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Control-plane REST API for the quiz life-cycle. The hot path (answer
 * submission, leaderboard polling) lives in other services — this controller
 * only handles authoring and quiz-start/stop commands.
 */
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final SessionOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<Quiz> create(@Valid @RequestBody CreateQuizRequest req) {
        return ResponseEntity.ok(quizService.create(req));
    }

    @GetMapping("/{id}")
    public Quiz get(@PathVariable String id) {
        return quizService.findById(id);
    }

    @PostMapping("/{quizId}/start")
    public QuizSession start(@PathVariable String quizId,
                             @RequestHeader(value = "X-Player-Id", defaultValue = "host") String hostId) {
        return orchestrator.startSession(quizId, hostId);
    }

    @PostMapping("/session/{sessionId}/begin")
    public QuizSession begin(@PathVariable String sessionId) {
        return orchestrator.beginSession(sessionId);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> notFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found", "message", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "conflict", "message", e.getMessage()));
    }
}

