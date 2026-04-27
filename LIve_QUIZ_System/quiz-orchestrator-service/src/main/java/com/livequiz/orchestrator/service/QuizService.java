package com.livequiz.orchestrator.service;

import com.livequiz.orchestrator.domain.*;
import com.livequiz.orchestrator.repository.QuizRepository;
import com.livequiz.orchestrator.web.dto.CreateQuizRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Handles quiz *template* CRUD. Deliberately minimal — this service is on the
 * authoring path, not the hot path, so we keep it simple and rely on JPA.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    @Transactional
    public Quiz create(CreateQuizRequest req) {
        Quiz quiz = Quiz.builder()
                .id(UUID.randomUUID().toString())
                .title(req.title())
                .hostId(req.hostId())
                .createdAt(Instant.now())
                .build();
        List<Question> questions = IntStream.range(0, req.questions().size())
                .mapToObj(i -> {
                    var q = req.questions().get(i);
                    return Question.builder()
                            .id(UUID.randomUUID().toString())
                            .quiz(quiz)
                            .text(q.text())
                            .options(q.options())
                            .correctOptionIndex(q.correctOptionIndex())
                            .timeLimitSeconds(q.timeLimitSeconds())
                            .orderIndex(i)
                            .build();
                }).toList();
        quiz.getQuestions().addAll(questions);
        return quizRepository.save(quiz);
    }

    @Transactional(readOnly = true)
    public Quiz findById(String id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("quiz not found: " + id));
    }
}

