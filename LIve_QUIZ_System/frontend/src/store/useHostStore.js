import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * Host-side state. Intentionally separate from the player store so that a
 * host and a player in the same browser (dev scenario) don't stomp on each
 * other's session identifiers.
 *
 * Shape:
 *   hostId           string          persisted pseudonym
 *   quizId           string | null   last created quiz
 *   sessionId        string | null   last started session (room code)
 *   currentQuestion  {...} | null    mirrors backend's QuestionPublishedEvent payload
 *   leaderboard      Entry[]         top N live
 *   quizEnded        boolean
 */
export const useHostStore = create(
  persist(
    (set) => ({
      hostId: null,
      quizId: null,
      sessionId: null,
      currentQuestion: null,
      leaderboard: [],
      quizEnded: false,

      setHostId: (hostId) => set({ hostId }),
      setQuizId: (quizId) => set({ quizId }),

      setSessionId: (sessionId) =>
        set({
          sessionId,
          currentQuestion: null,
          leaderboard: [],
          quizEnded: false,
        }),

      setQuestion: (q) => set({ currentQuestion: q, quizEnded: false }),
      setLeaderboard: (entries) => set({ leaderboard: entries }),
      setQuizEnded: (v) => set({ quizEnded: v }),

      reset: () =>
        set({
          quizId: null,
          sessionId: null,
          currentQuestion: null,
          leaderboard: [],
          quizEnded: false,
        }),
    }),
    {
      name: 'live-quiz-host-state',
      partialize: (s) => ({ hostId: s.hostId, quizId: s.quizId, sessionId: s.sessionId }),
    }
  )
);

/** Generate a stable opaque host id if one hasn't been picked yet. */
export function ensureHostId(set) {
  const { hostId, setHostId } = set;
  if (hostId) return hostId;
  const id = 'host_' + Math.random().toString(36).slice(2, 10);
  setHostId(id);
  return id;
}

