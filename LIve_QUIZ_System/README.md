# Live Quiz System — Real-time Distributed Quiz Platform

Production-grade, horizontally-scalable, real-time quiz platform (Kahoot-style)
built as Java Spring Boot microservices, designed for an academic
**system-design + backend-engineering** submission.

---

## 1. What it does

* **Live quiz sessions** — a host creates a quiz, starts a session, and an
  orchestrator advances questions on a timer.
* **Real-time question broadcast** — pushed to every connected player over
  WebSocket (STOMP) within tens of milliseconds of publish.
* **Answer submission with latency tracking** — response time is measured
  server-side (players can't cheat the clock).
* **Live leaderboard** — recomputed every answer, streamed to all players in
  the session.
* **Anti-cheat** — tab-switch, window-blur, rapid-answer and multi-session
  signals are scored; flagged players are disqualified at a threshold.

---

## 2. Architecture

```
                        +------------------+
                        |   API Gateway    | (Spring Cloud Gateway, JWT, rate-limit)
                        +---------+--------+
                                  |
       +-----------------+--------+--------+----------------+-------------------+
       |                 |                 |                |                   |
+------v------+  +-------v------+  +-------v------+  +------v-------+  +--------v--------+
|   Quiz      |  |   Player     |  |   Answer     |  | Leaderboard  |  |  Anti-Cheat     |
|Orchestrator |  |   Session    |  |  Processing  |  |              |  |                 |
| (Postgres)  |  |   (Redis)    |  |(Redis+Mongo) |  |   (Redis)    |  |    (Mongo)      |
+------+------+  +--------------+  +------+-------+  +------+-------+  +--------+--------+
       |                                  |                 ^                   ^
       |     Kafka  (quiz.started,        |                 |                   |
       |             question.published,  |                 |                   |
       |             quiz.ended)          v                 |                   |
       +-----------------------------> [Kafka Broker] <-----+-------------------+
                                            ^                ^
                                            |                |
                                            |           (LEADERBOARD_UPDATED via
                                            |            Redis Pub/Sub channel)
                                            |                |
                                      +-----+----------------+------+
                                      |     WebSocket Gateway       |
                                      |   (STOMP, Redis Pub/Sub)    |
                                      +--------------+--------------+
                                                     |
                                               (persistent WS)
                                                     |
                                                  Clients
```

### Services & ports

| Service                     | Port | Primary backing store |
|----------------------------|------|-----------------------|
| api-gateway                | 8080 | Redis (rate-limiter)  |
| quiz-orchestrator-service  | 8081 | PostgreSQL            |
| player-session-service     | 8082 | Redis                 |
| answer-processing-service  | 8083 | Redis + MongoDB       |
| leaderboard-service        | 8084 | Redis (ZSET + Pub/Sub)|
| anti-cheat-service         | 8085 | MongoDB               |
| websocket-gateway-service  | 8086 | Redis Pub/Sub, Kafka  |

---

## 3. Why each technology

| Concern                        | Choice                            | Why |
|-------------------------------|-----------------------------------|-----|
| Inter-service comms           | **Kafka**                         | Durable event log, decouples producers/consumers, enables replay + new consumers without touching producers. Direct REST calls would couple services and destroy fault-isolation. |
| Session cache & roster        | **Redis (strings + sets)**        | Sub-ms reads, TTL-based GC of stale sessions, perfect for ephemeral "who's online" data. |
| Leaderboard                   | **Redis Sorted Set (ZSET)**       | `ZINCRBY` / `ZREVRANGE` are O(log N); the canonical pattern for real-time scoreboards. |
| Real-time fan-out             | **Redis Pub/Sub + STOMP**         | Every WS node subscribes; one `PUBLISH` reaches all nodes. Simpler than a full STOMP relay, and good enough up to a few dozen gateway replicas. |
| Relational (quizzes, results) | **PostgreSQL**                    | ACID authoring + final scores; natural fit for the typed, relational quiz/question/result data. |
| High-write audit logs         | **MongoDB**                       | Schema-flexible (we tweak cheat-signal fields often), great append-only write throughput, cheap aggregations. |
| Transport to browser          | **WebSocket / STOMP / SockJS**    | Bi-directional, long-lived; SockJS fallback rescues clients behind strict proxies. |
| Auth                          | **JWT at the gateway**            | Downstream services only read `X-Player-Id` headers; single point of crypto. |

---

## 4. Key trade-offs

* **Eventual consistency on the leaderboard.** We update ZSET *per answer*
  and publish via Pub/Sub (not guaranteed delivery). A lost packet is rare,
  costs at most ~50ms, and is healed by the next event. The alternative —
  synchronous RPC on every submission — would bottleneck at 10K players.
* **At-least-once Kafka delivery.** Producers use `acks=all` + idempotence;
  consumers are idempotent-friendly (de-dup Redis key on answers, upsert ZSET
  by playerId). We explicitly don't try to achieve exactly-once across
  services because the extra complexity is unnecessary for the invariants
  we care about.
* **Redis as a single-point-of-failure-ish component.** In production we'd
  run Redis Cluster with sentinels; the app code already uses
  `spring-boot-starter-data-redis` so swapping in is zero-code.
* **Coarse (1-sec) scheduler tick in the orchestrator.** Good enough for
  human-timed questions (10–60 s). If sub-second accuracy is ever required,
  we'd move to per-session delayed tasks on a `ScheduledExecutorService`
  or Kafka-Streams timer wheel.
* **No ML in anti-cheat.** Explainable, rule-based scoring; easy to justify
  to a teacher/admin. Can be upgraded to a streaming ML model behind the
  same Kafka contract.

---

## 5. Scaling strategy

* **All services are stateless.** Horizontal scaling = add replicas behind a
  load balancer (no sticky sessions required, even for WebSockets).
* **Kafka** is partitioned by `sessionId`, so the same session's ordering is
  preserved within a partition and multiple sessions parallelise across
  consumers. 3–6 partitions per topic is a sensible start.
* **WebSocket Gateway** scales by adding nodes; each is its own Kafka
  consumer group (see `KafkaConfig` — group id has a UUID suffix) so every
  node receives every event and delivers to its own clients.
* **Redis Pub/Sub** carries the leaderboard fan-out — one publish reaches N
  WS nodes in parallel. At ≥ ~50 gateway nodes we'd migrate to a proper
  STOMP broker relay (RabbitMQ / ActiveMQ).
* **Target: 10K concurrent users.** At 4 gateway nodes × ~2.5K connections
  each, with Kafka at 3 partitions and a single Redis instance, headroom is
  comfortable for answer bursts of ~10K/s for short windows.

---

## 6. End-to-end flow (happy path)

1. Host `POST /api/quiz` (via Gateway → Orchestrator) — creates quiz with
   questions in Postgres.
2. Host `POST /api/quiz/{id}/start` — Orchestrator:
   * persists `QuizSession` row (status=RUNNING)
   * emits `quiz.started`
   * emits `quiz.question.published` for question 0
3. WebSocket Gateway nodes consume `quiz.question.published` and broadcast
   a client-safe payload (no correct answer!) to
   `/topic/session/{sid}/question`.
4. Answer Processing service consumes the same event and caches
   `question:{qid}:correct` + `question:{qid}:published` in Redis.
5. Player sends STOMP `/app/answer` frame → Gateway → REST POST to Answer
   Processing → validates, dedups, computes server-side latency → emits
   `quiz.answer.submitted`.
6. Leaderboard consumes `quiz.answer.submitted` → `ZINCRBY` →
   `PUBLISH leaderboard.updates` → WS Gateway re-broadcasts to
   `/topic/session/{sid}/leaderboard`.
7. Meanwhile the Anti-Cheat service consumes both `quiz.behavior.events` and
   `quiz.answer.submitted` — if a player's running suspicion score crosses a
   threshold, it emits `quiz.cheating.detected`.
8. Orchestrator's scheduler tick moves to the next question once the time
   limit elapses; at the end, `quiz.ended` is emitted and final `Result`
   rows can be persisted in Postgres.

---

## 7. Data models

### PostgreSQL (quiz-orchestrator)

* `quiz(id, title, host_id, created_at)`
* `question(id, quiz_id, text, correct_option_index, order_index, time_limit_seconds)`
* `question_option(question_id, option_index, option_text)`
* `quiz_session(id, quiz_id, status, current_question_index, current_question_published_at, started_at, ended_at)`
* `result(id, session_id, player_id, score, correct_answers, rank)`

### Redis

* `session:{sid}:player:{pid}` — JSON PlayerSession (TTL 30m)
* `session:{sid}:roster` — Set<playerId>
* `question:{qid}:correct` — int (TTL ≈ question window)
* `question:{qid}:published` — ISO instant
* `answered:{qid}:{pid}` — dedup sentinel
* `quiz:{sid}:leaderboard` — ZSET<playerId → score>
* `quiz:{sid}:names` — Hash<playerId → displayName>
* Pub/Sub channel: `leaderboard.updates`

### MongoDB

* `answer_logs` — audit of every submission
* `cheat_logs` — audit of every suspicious signal

---

## 8. Kafka topics

| Topic                       | Producer              | Consumers                               |
|----------------------------|-----------------------|-----------------------------------------|
| `quiz.started`             | orchestrator          | ws-gateway (future), leaderboard init   |
| `quiz.question.published`  | orchestrator          | ws-gateway, answer-processing           |
| `quiz.ended`               | orchestrator          | leaderboard (final persistence)         |
| `quiz.answer.submitted`    | answer-processing     | leaderboard, anti-cheat                 |
| `quiz.leaderboard.updated` | (published via Pub/Sub)| ws-gateway                              |
| `quiz.behavior.events`     | ws-gateway            | anti-cheat                              |
| `quiz.cheating.detected`   | anti-cheat            | orchestrator, ws-gateway (alerts)       |

---

## 9. Running locally

```bash
# 1. Build everything
mvn -DskipTests package

# 2. Bring up infra + services
docker compose up --build
```

Gateway is exposed at `http://localhost:8080`. STOMP endpoint:
`ws://localhost:8080/ws/websocket`.

### Smoke test

```bash
# Create quiz
curl -H 'Content-Type: application/json' -d @sample-quiz.json \
     http://localhost:8080/api/quiz

# Start it
curl -X POST http://localhost:8080/api/quiz/{quizId}/start

# Join from a client (STOMP) and subscribe to
#   /topic/session/{sessionId}/question
#   /topic/session/{sessionId}/leaderboard
```

---

## 10. Bonus features delivered

* **Reconnect logic** — Redis sessions keyed by `(sessionId, playerId)` with
  a 30-minute TTL. A re-join with the same IDs transparently resumes.
* **Timer synchronization** — `QuestionPublishedEvent.publishedAt` is the
  server-authoritative clock; clients compute remaining time as
  `timeLimit - (now - publishedAt)`.
* **Admin dashboard APIs** — `/api/quiz/admin/sessions/active` and
  `/api/quiz/admin/sessions/{id}/results` return live + final state; anti-
  cheat exposes `/api/anticheat/{sessionId}/players/{playerId}/logs`.

---

## 11. Project layout

```
live-quiz-system/
├── pom.xml                        # parent POM
├── docker-compose.yml             # Kafka, Redis, Postgres, Mongo + services
├── common/                        # shared DTOs, Kafka event records, topic names
├── api-gateway/
├── quiz-orchestrator-service/
├── player-session-service/
├── answer-processing-service/
├── leaderboard-service/
├── anti-cheat-service/
└── websocket-gateway-service/
```

Each service follows `controller → service → repository` layering and is an
independently-deployable Spring Boot jar.

