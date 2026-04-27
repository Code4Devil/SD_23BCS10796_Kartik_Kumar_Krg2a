package com.livequiz.anticheat.repository;

import com.livequiz.anticheat.model.CheatLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CheatLogRepository extends MongoRepository<CheatLog, String> {
    List<CheatLog> findBySessionIdAndPlayerId(String sessionId, String playerId);
    List<CheatLog> findBySessionId(String sessionId);
}

