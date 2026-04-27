package com.livequiz.orchestrator.repository;

import com.livequiz.orchestrator.domain.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSessionRepository extends JpaRepository<QuizSession, String> {
    List<QuizSession> findByStatus(QuizSession.Status status);
}

