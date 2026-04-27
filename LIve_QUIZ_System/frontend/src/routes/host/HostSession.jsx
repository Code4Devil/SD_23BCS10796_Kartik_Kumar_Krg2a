import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Card from '../../components/Card';
import Button from '../../components/Button';
import Timer from '../../components/Timer';
import LeaderboardList from '../../components/LeaderboardList';
import { useHostStore } from '../../store/useHostStore';
import { useHostSessionWire } from '../../hooks/useHostSessionWire';
import { useTimer } from '../../hooks/useTimer';
import { beginQuiz } from '../../services/quizApi';
import { getLeaderboardTop } from '../../services/leaderboardApi';
import { getRoster } from '../../services/sessionApi';
import { getCheatStats } from '../../services/anticheatApi';

/**
 * Live presenter dashboard. Starts as a lobby showing the room code and the
 * joined-players roster; once the host presses "Start quiz" the orchestrator
 * takes over and advances questions on its own timer.
 */
export default function HostSession() {
  const { sessionId: routeSid } = useParams();
  const nav = useNavigate();
  const {
    sessionId,
    hostId,
    currentQuestion,
    leaderboard,
    setSessionId,
    setLeaderboard,
    quizEnded,
    setQuizEnded,
  } = useHostStore();

  const [beginning, setBeginning] = useState(false);
  const [beginError, setBeginError] = useState(null);

  // Hydrate store from route param on deep-link / refresh.
  useEffect(() => {
    if (routeSid && routeSid !== sessionId) setSessionId(routeSid);
  }, [routeSid, sessionId, setSessionId]);

  useHostSessionWire();

  const [roster, setRoster] = useState([]);
  const [copied, setCopied] = useState(false);
  const [cheatStats, setCheatStats] = useState([]);

  useEffect(() => {
    if (!sessionId) return;
    getLeaderboardTop(sessionId)
      .then((d) => Array.isArray(d) && setLeaderboard(d))
      .catch(() => {});
  }, [sessionId, setLeaderboard]);

  useEffect(() => {
    if (!sessionId) return;
    let alive = true;
    const load = () =>
      getRoster(sessionId)
        .then((r) => alive && setRoster(Array.isArray(r) ? r : Array.from(r ?? [])))
        .catch(() => {});
    load();
    const id = setInterval(load, 3_000);
    return () => { alive = false; clearInterval(id); };
  }, [sessionId]);

  // Poll anti-cheat aggregates. Mongo-backed query is cheap (indexed on
  // sessionId) and the host dashboard is a low-cardinality view.
  useEffect(() => {
    if (!sessionId) return;
    let alive = true;
    const load = () =>
      getCheatStats(sessionId)
        .then((s) => alive && setCheatStats(Array.isArray(s) ? s : []))
        .catch(() => {});
    load();
    const id = setInterval(load, 3_000);
    return () => { alive = false; clearInterval(id); };
  }, [sessionId]);

  // Quiz-end is signalled authoritatively by the backend over
  // /topic/session/{sid}/ended (wired in useHostSessionWire). No local
  // inference needed.

  const remaining = useTimer(
    currentQuestion?.publishedAt,
    currentQuestion?.timeLimitSeconds
  );

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(sessionId);
      setCopied(true);
      setTimeout(() => setCopied(false), 1_500);
    } catch { /* ignore */ }
  };

  const begin = async () => {
    if (!sessionId) return;
    setBeginning(true);
    setBeginError(null);
    try {
      await beginQuiz(sessionId, hostId ?? 'host');
    } catch (e) {
      const status = e?.response?.status;
      const serverMsg = e?.response?.data?.message;
      if (status === 409) {
        setBeginError(serverMsg || 'This session has already ended. Create a new quiz to start again.');
      } else if (status === 404) {
        setBeginError('Session no longer exists. Please create a new quiz.');
      } else {
        setBeginError(serverMsg || e.message || 'Could not start the quiz');
      }
    } finally {
      setBeginning(false);
    }
  };

  const inLobby = !currentQuestion && !quizEnded;

  return (
    <div className="max-w-5xl mx-auto mt-4 space-y-5">
      {/* Giant room code banner — the primary thing players need. */}
      <Card className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-wider text-slate-500">
            Room code
          </p>
          <p className="text-3xl sm:text-4xl font-black font-mono break-all">
            {sessionId}
          </p>
          <p className="text-xs text-slate-500 mt-1">
            {roster.length} player{roster.length === 1 ? '' : 's'} joined
          </p>
        </div>
        <div className="flex gap-2">
          <Button onClick={copyCode}>{copied ? 'Copied!' : 'Copy code'}</Button>
          <Button variant="ghost" className="text-slate-900" onClick={() => nav('/host')}>
            Exit
          </Button>
        </div>
      </Card>

      <div className="grid gap-5 lg:grid-cols-5">
        <div className="lg:col-span-3 space-y-3">
          <h2 className="text-2xl font-black">
            {inLobby ? 'Lobby' : 'Current question'}
          </h2>
          {currentQuestion ? (
            <Card>
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs uppercase text-slate-500">
                  Q{currentQuestion.questionIndex + 1}
                </span>
                <span className="text-xs text-slate-500">
                  {currentQuestion.timeLimitSeconds}s limit
                </span>
              </div>
              <h3 className="text-xl font-black mb-4">{currentQuestion.text}</h3>
              <Timer
                seconds={remaining}
                total={currentQuestion.timeLimitSeconds}
              />
              <ul className="mt-4 grid sm:grid-cols-2 gap-2">
                {currentQuestion.options.map((o, i) => (
                  <li key={i} className="bg-slate-100 rounded-lg px-3 py-2 text-slate-800">
                    {String.fromCharCode(65 + i)}. {o}
                  </li>
                ))}
              </ul>
              <p className="text-xs text-slate-400 mt-3">
                Correct answer is hidden from the host view too — it lives only
                in the Answer Processing service's Redis cache.
              </p>
            </Card>
          ) : inLobby ? (
            <Card className="text-center space-y-4">
              <p className="text-slate-700">
                Room is open. Waiting for players — share the room code above.
              </p>
              <p className="text-slate-500 text-sm">
                {roster.length === 0
                  ? 'No players yet.'
                  : `Ready when you are — ${roster.length} player${roster.length === 1 ? '' : 's'} in the room.`}
              </p>
              <Button
                onClick={begin}
                disabled={beginning}
                className="text-lg px-8 py-4"
              >
                {beginning ? 'Starting…' : '▶ Start quiz'}
              </Button>
              {beginError && (
                <p className="text-sm text-brand-red font-semibold">{beginError}</p>
              )}
            </Card>
          ) : (
            <Card><p className="text-slate-600">Waiting for next question…</p></Card>
          )}

          {quizEnded && (
            <Card className="bg-brand-green/15 border-l-4 border-green-500">
              <p className="font-black text-green-800">🏁 Quiz finished</p>
              <p className="text-sm text-slate-700">
                Final results are being persisted to the Result table.
              </p>
            </Card>
          )}
        </div>

        <div className="lg:col-span-2 space-y-3">
          <h2 className="text-2xl font-black">🏆 Live leaderboard</h2>
          <LeaderboardList entries={leaderboard} />

          <h2 className="text-2xl font-black pt-3">🛡 Integrity</h2>
          <CheatStatsList entries={cheatStats} leaderboard={leaderboard} />
        </div>
      </div>
    </div>
  );
}

/**
 * Per-player anti-cheat counter panel shown next to the live leaderboard.
 * Zero-count rows are hidden to keep the list quiet until someone trips a
 * signal; a resolved name is looked up from the leaderboard so the host sees
 * human display names rather than opaque playerIds.
 */
function CheatStatsList({ entries, leaderboard }) {
  if (!entries?.length) {
    return (
      <Card className="py-3">
        <p className="text-sm text-slate-500">No anti-cheat signals yet.</p>
      </Card>
    );
  }
  const nameOf = (pid) =>
    leaderboard.find((e) => e.playerId === pid)?.displayName || pid.slice(0, 8);
  const LABELS = {
    TAB_HIDDEN: 'tab',
    WINDOW_BLUR: 'blur',
    FULLSCREEN_EXIT: 'fs-exit',
    COPY: 'copy',
    PASTE: 'paste',
    RAPID_ANSWER: 'rapid',
    MULTIPLE_SESSION_DETECTED: 'multi',
  };
  return (
    <ul className="space-y-2">
      {entries
        .slice()
        .sort((a, b) => b.totalSeverity - a.totalSeverity)
        .map((s) => (
          <li
            key={s.playerId}
            className="bg-white/15 rounded-xl px-3 py-2 text-white"
          >
            <div className="flex items-center justify-between">
              <span className="font-semibold truncate">{nameOf(s.playerId)}</span>
              <span className="text-xs font-mono opacity-80">
                score {s.totalSeverity}
              </span>
            </div>
            <div className="flex flex-wrap gap-1 mt-1 text-xs">
              {Object.entries(s.counts || {}).map(([k, n]) => (
                <span
                  key={k}
                  className="px-2 py-0.5 rounded-full bg-white/20 tabular-nums"
                >
                  {LABELS[k] ?? k.toLowerCase()}: {n}
                </span>
              ))}
            </div>
          </li>
        ))}
    </ul>
  );
}

