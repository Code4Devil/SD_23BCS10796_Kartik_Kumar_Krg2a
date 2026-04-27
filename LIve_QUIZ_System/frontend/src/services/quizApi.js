import { http } from './api';

/** POST /api/quiz — authoring (host only). */
export const createQuiz = async (payload) => {
  const { data } = await http.post('/api/quiz', payload);
  if (!data || !data.id) {
    // eslint-disable-next-line no-console
    console.error('createQuiz: server returned body without id:', data);
    throw new Error('Server returned a quiz without an id');
  }
  return data;
};

/** GET /api/quiz/{id} */
export const getQuiz = (quizId) =>
  http.get(`/api/quiz/${quizId}`).then((r) => r.data);

/** POST /api/quiz/{quizId}/start — returns QuizSession {id,...} (the id is our room/session code). */
export const startQuiz = (quizId, hostId = 'host') =>
  http
    .post(`/api/quiz/${quizId}/start`, null, { headers: { 'X-Player-Id': hostId } })
    .then((r) => r.data);

/** POST /api/quiz/session/{sessionId}/begin — transitions a lobby session to RUNNING and publishes Q1. */
export const beginQuiz = (sessionId, hostId = 'host') =>
  http
    .post(`/api/quiz/session/${sessionId}/begin`, null, { headers: { 'X-Player-Id': hostId } })
    .then((r) => r.data);

/** GET /api/quiz/admin/sessions/{sessionId}/results — final score list. */
export const getSessionResults = (sessionId) =>
  http.get(`/api/quiz/admin/sessions/${sessionId}/results`).then((r) => r.data);

