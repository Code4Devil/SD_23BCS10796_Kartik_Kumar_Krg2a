package com.livequiz.orchestrator.repository;

import com.livequiz.orchestrator.domain.Result;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findBySessionIdOrderByRankAsc(String sessionId);
}

