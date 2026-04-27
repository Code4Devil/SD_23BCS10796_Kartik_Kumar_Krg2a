package com.livequiz.orchestrator.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "question")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    @JsonIgnore
    private Quiz quiz;

    @Column(nullable = false, length = 2000)
    private String text;

    /**
     * Options stored as a simple element-collection table. For very large
     * quizzes we'd split this out; for typical <= 10-option MCQs this keeps
     * the schema simple and queries a single-hop fetch.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_option", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "option_text", length = 500)
    @OrderColumn(name = "option_index")
    private List<String> options;

    @Column(nullable = false)
    private int correctOptionIndex;

    @Column(nullable = false)
    private int orderIndex;

    @Column(nullable = false)
    private int timeLimitSeconds;
}

