package com.livequiz.orchestrator.web;

import com.livequiz.orchestrator.domain.QuizSession;
import com.livequiz.orchestrator.domain.Result;
import com.livequiz.orchestrator.repository.QuizSessionRepository;
import com.livequiz.orchestrator.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin/dashboard endpoints. Deliberately separate from the authoring API
 * so RBAC can be applied independently (e.g. X-Player-Role=ADMIN only).
 */
@RestController
@RequestMapping("/api/quiz/admin")
@RequiredArgsConstructor
public class AdminController {

    private final QuizSessionRepository sessions;
    private final ResultRepository results;

    @GetMapping("/sessions/active")
    public List<QuizSession> active() {
        return sessions.findByStatus(QuizSession.Status.RUNNING);
    }

    @GetMapping("/sessions/{sessionId}/results")
    public List<Result> results(@PathVariable String sessionId) {
        return results.findBySessionIdOrderByRankAsc(sessionId);
    }
}

