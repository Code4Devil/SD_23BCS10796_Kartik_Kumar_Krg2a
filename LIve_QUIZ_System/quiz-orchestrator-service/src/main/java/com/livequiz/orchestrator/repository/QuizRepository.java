package com.livequiz.orchestrator.repository;

import com.livequiz.orchestrator.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, String> {}

