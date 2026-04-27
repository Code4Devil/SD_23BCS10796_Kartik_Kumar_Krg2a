# LiveQuiz Frontend

Vite + React + Tailwind + Zustand client for the LiveQuiz backend.

## Prerequisites

- Node 18+
- The backend running locally (`docker compose up --build` from repo root).
  Gateway on `:8080`, WebSocket gateway on `:8086`.

## Setup

```bash
cd frontend
cp .env.example .env      # edit if your gateway isn't on :8080
npm install
npm run dev               # http://localhost:5173
```

## Environment

| Variable         | Purpose                                          | Default                     |
|------------------|--------------------------------------------------|-----------------------------|
| `VITE_API_BASE`  | REST base URL (normally the api-gateway)         | `http://localhost:8080`     |
| `VITE_WS_URL`    | SockJS endpoint                                  | `http://localhost:8080/ws`  |
| `VITE_JWT_TOKEN` | Bearer token for gateway-protected REST routes   | *(empty)*                   |

The api-gateway requires a JWT on `/api/quiz/**`, `/api/session/**`,
`/api/answer/**` and `/api/anticheat/**`. For local tinkering you can bypass
it by pointing `VITE_API_BASE` at an individual service (e.g. the player
session service on `:8082`).

## Project layout

```
src/
├── main.jsx              # React root + BrowserRouter
├── App.jsx               # routes + SessionGuard (mounts WS wire-up)
├── index.css             # Tailwind + component primitives
├── routes/
│   ├── Home.jsx          # player: enter name + room code → joinSession
│   ├── Waiting.jsx       # player: live roster; auto-jumps on first question
│   ├── Quiz.jsx          # player: question + options + timer + anti-cheat
│   ├── Leaderboard.jsx   # player: live ranking (WS-driven)
│   ├── Result.jsx        # player: final score + rank + full board
│   └── host/
│       ├── HostHome.jsx     # host landing + identity
│       ├── HostCreate.jsx   # authoring form (title + N questions)
│       ├── HostPreview.jsx  # preview + Start session
│       └── HostSession.jsx  # live presenter dashboard
├── components/           # Card, Button, OptionCard, Timer, PlayerList,
│                         # LeaderboardList, Layout
├── hooks/
│   ├── useSessionWire.js # opens WS, subscribes to all session topics
│   ├── useTimer.js       # server-clock-anchored countdown
│   └── useAntiCheat.js   # visibilitychange / blur / copy-paste → behavior
├── services/
│   ├── api.js            # axios + JWT interceptor
│   ├── quizApi.js        # /api/quiz/**
│   ├── sessionApi.js     # /api/session/**
│   ├── leaderboardApi.js # /api/leaderboard/**
│   ├── answerApi.js      # /api/answer/** (REST fallback)
│   └── ws.js             # STOMP-over-SockJS singleton
└── store/useQuizStore.js # Zustand (user + sessionId persisted)
```

## Flow

### Host

1. Open `/host` (there's a toggle in the header too). Pick a host id.
2. `/host/create` — fill in title + questions, or click **Load sample** for a
   working demo. Submit → `POST /api/quiz`.
3. `/host/quiz/:quizId` — preview the questions. The correct answer is
   highlighted locally (it's returned by `GET /api/quiz/{id}`). Click
   **Start live session** → `POST /api/quiz/{id}/start`.
4. `/host/session/:sessionId` — the live presenter dashboard. Shows the
   room code (big), current question + server-synced timer, the connected
   player count, and the live WS-driven leaderboard.

> ⚠️ **No lobby window.** The orchestrator's `startSession` emits question 0
> immediately, so share the room code with players *before* you press Start,
> or design your first question as a welcome slide with a generous time limit.

### Player

1. `/` — enter display name + room code (the sessionId the host just
   received). `POST /api/session/join` registers the player.
2. `/waiting` — live roster via `GET /api/session/{sid}/roster` (3 s poll).
   When the first `/topic/session/{sid}/question` arrives, auto-jumps to…
3. `/quiz` — question card, coloured option tiles, server-clock-anchored
   countdown. Tapping an option sends `/app/answer` over STOMP.
4. `/leaderboard` — live `/topic/session/{sid}/leaderboard` stream.
5. `/result` — when no further questions arrive for 6 s after the current
   question's timer ends, routes here and polls
   `/api/quiz/admin/sessions/{sid}/results` for the durable final table.

## Anti-cheat

`useAntiCheat` forwards the following signals over `/app/behavior`:
`TAB_HIDDEN`, `TAB_VISIBLE`, `WINDOW_BLUR`, `WINDOW_FOCUS`, `COPY`, `PASTE`.
If the anti-cheat service pushes a `/topic/session/{sid}/cheat-alert` frame,
a toast is shown at the bottom of every screen.

## Notes / trade-offs

- **No page reloads:** all inter-screen transitions happen via React Router
  state + Zustand; the WS subscription lives on `App` and survives route
  changes.
- **Reconnect:** `@stomp/stompjs` auto-reconnects every 3s. The axios
  heartbeat every 10s keeps the Redis session TTL fresh.
- **Late join:** `joinSession` is idempotent on the server; a late player
  simply starts receiving the next published question.
- **QUIZ_ENDED detection:** the WS gateway does not currently rebroadcast
  `quiz.ended` on a client topic, so the client infers it from a grace
  window after the last question. If you wire up a `/topic/session/{sid}/ended`
  topic on the backend, update `useSessionWire` to subscribe and call
  `setQuizEnded(true)` directly.

