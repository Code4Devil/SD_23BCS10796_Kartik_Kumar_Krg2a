import { useEffect } from 'react';
import { ws } from '../services/ws';
import { useQuizStore } from '../store/useQuizStore';
import { heartbeat } from '../services/sessionApi';

/**
 * Central subscription wire-up for a session. Intended to be mounted by the
 * top-level route guard so all session-scoped routes share one set of subs.
 *
 * Responsibilities:
 *   1. Open (or reuse) the WS connection.
 *   2. Subscribe to question / leaderboard / cheat-alert topics.
 *   3. Drive Zustand state from inbound frames.
 *   4. Fire a periodic REST heartbeat so the server keeps the session alive.
 */
export function useSessionWire() {
  const {
    sessionId,
    user,
    setQuestion,
    setLeaderboard,
    setCheatAlert,
    setConnected,
    setQuizEnded,
  } = useQuizStore();

  useEffect(() => {
    if (!sessionId || !user?.playerId) return;

    ws.connect();

    const offConn = ws.onConnect(() => setConnected(true));
    const offDisc = ws.onDisconnect(() => setConnected(false));

    const offQ = ws.subscribeQuestion(sessionId, (payload) => {
      if (!payload) return;
      setQuestion(payload);
    });

    const offLb = ws.subscribeLeaderboard(sessionId, (payload) => {
      if (!payload?.top) return;
      setLeaderboard(payload.top);
    });

    const offCa = ws.subscribeCheatAlert(sessionId, (payload) => {
      const msg =
        typeof payload === 'string'
          ? payload
          : payload?.reason || 'Suspicious activity detected';
      setCheatAlert(msg);
    });

    const offEnd = ws.subscribeQuizEnded(sessionId, () => {
      setQuizEnded(true);
    });

    // Keep the Redis-backed session TTL fresh while the tab is open.
    const hb = setInterval(() => {
      heartbeat(sessionId, user.playerId).catch(() => {});
    }, 10_000);

    return () => {
      offConn();
      offDisc();
      offQ();
      offLb();
      offCa();
      offEnd();
      clearInterval(hb);
    };
  }, [sessionId, user?.playerId, setQuestion, setLeaderboard, setCheatAlert, setConnected, setQuizEnded]);
}

