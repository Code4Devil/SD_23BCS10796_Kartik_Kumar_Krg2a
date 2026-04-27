import { useEffect } from 'react';
import { ws } from '../services/ws';
import { useHostStore } from '../store/useHostStore';

/**
 * Host-scoped WS subscription wire-up. Like `useSessionWire` but without the
 * player heartbeat and anti-cheat plumbing — a host is a pure observer in
 * this backend (orchestrator advances questions on its own scheduler tick).
 */
export function useHostSessionWire() {
  const { sessionId, setQuestion, setLeaderboard, setQuizEnded } = useHostStore();

  useEffect(() => {
    if (!sessionId) return;

    ws.connect();

    const offQ = ws.subscribeQuestion(sessionId, (payload) => {
      if (payload) setQuestion(payload);
    });
    const offLb = ws.subscribeLeaderboard(sessionId, (payload) => {
      if (payload?.top) setLeaderboard(payload.top);
    });
    const offEnd = ws.subscribeQuizEnded(sessionId, () => {
      setQuizEnded(true);
    });

    return () => {
      offQ();
      offLb();
      offEnd();
    };
  }, [sessionId, setQuestion, setLeaderboard, setQuizEnded]);
}

