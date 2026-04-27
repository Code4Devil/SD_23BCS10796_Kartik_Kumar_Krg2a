package com.livequiz.orchestrator.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root for a quiz *template*. Not to be confused with a `Session`,
 * which is one run-through of a quiz. Separating the two lets the same quiz
 * be replayed repeatedly (classroom re-runs, A/B comparison, etc.) without
 * duplicating question content.
 */
@Entity
@Table(name = "quiz")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quiz {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    private String hostId;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;
}

