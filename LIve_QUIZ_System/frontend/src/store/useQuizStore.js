import { create } from 'zustand';
import { persist } from 'zustand/middleware';

/**
 * Single global store for the whole quiz UI.
 *
 * Shape:
 *   user            { playerId, displayName }
 *   sessionId       string | null      // room code
 *   connected       boolean            // WS state
 *   roster          string[]           // player ids currently in the room
 *   currentQuestion { questionId, questionIndex, text, options, timeLimitSeconds, publishedAt } | null
 *   answeredFor     Set<questionId>    // prevents double-submit
 *   leaderboard     Entry[]            // [{playerId, displayName, score, rank}]
 *   quizEnded       boolean
 *   cheatAlert      string | null      // last cheat-alert message shown to user
 */
export const useQuizStore = create(
  persist(
    (set, get) => ({
      user: null,
      sessionId: null,
      connected: false,
      roster: [],
      currentQuestion: null,
      answeredFor: [],
      leaderboard: [],
      quizEnded: false,
      cheatAlert: null,

      setUser: (user) => set({ user }),
      setSessionId: (sessionId) => set({ sessionId }),
      setConnected: (connected) => set({ connected }),
      setRoster: (roster) => set({ roster }),

      setQuestion: (q) =>
        set((s) => ({
          currentQuestion: q,
          quizEnded: false,
          // If this is a new question we haven't answered it yet — no state change
          // needed on answeredFor because we only ever APPEND to it on submit.
          cheatAlert: s.cheatAlert,
        })),

      markAnswered: (questionId) =>
        set((s) => ({
          answeredFor: s.answeredFor.includes(questionId)
            ? s.answeredFor
            : [...s.answeredFor, questionId],
        })),

      hasAnswered: (questionId) => get().answeredFor.includes(questionId),

      setLeaderboard: (entries) => set({ leaderboard: entries }),
      setQuizEnded: (v) => set({ quizEnded: v }),
      setCheatAlert: (msg) => set({ cheatAlert: msg }),

      resetSession: () =>
        set({
          sessionId: null,
          roster: [],
          currentQuestion: null,
          answeredFor: [],
          leaderboard: [],
          quizEnded: false,
          cheatAlert: null,
          connected: false,
        }),
    }),
    {
      name: 'live-quiz-state',
      // Only the identity + room survive a refresh — runtime state is rebuilt
      // from the WS subscription, which is the source of truth.
      partialize: (s) => ({ user: s.user, sessionId: s.sessionId }),
    }
  )
);

