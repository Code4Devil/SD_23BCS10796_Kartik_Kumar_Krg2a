package com.livequiz.answer.repository;

import com.livequiz.answer.model.AnswerLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnswerLogRepository extends MongoRepository<AnswerLog, String> {}

