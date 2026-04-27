package com.livequiz.answer.web;

import com.livequiz.answer.service.AnswerService;
import com.livequiz.common.dto.AnswerSubmissionDto;
import com.livequiz.common.events.AnswerSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/answer")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    /**
     * REST fallback for answer submission. The preferred hot path is a
     * WebSocket frame that the WS Gateway forwards internally — but we keep
     * a REST endpoint too for clients that cannot hold a WS open (mobile
     * push-to-foreground, server-to-server load tests, etc.).
     */
    @PostMapping("/submit")
    public ResponseEntity<AnswerSubmittedEvent> submit(@RequestBody AnswerSubmissionDto dto) {
        try {
            return ResponseEntity.ok(answerService.submit(dto));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

