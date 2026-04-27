import { http } from './api';

/** POST /api/session/join — idempotent (server transparently re-joins). */
export const joinSession = ({ sessionId, playerId, displayName }) =>
  http
    .post('/api/session/join', { sessionId, playerId, displayName })
    .then((r) => r.data);

/** POST /api/session/{sessionId}/heartbeat */
export const heartbeat = (sessionId, playerId) =>
  http
    .post(`/api/session/${sessionId}/heartbeat`, null, {
      headers: { 'X-Player-Id': playerId },
    })
    .then((r) => r.data);

/** POST /api/session/{sessionId}/leave */
export const leaveSession = (sessionId, playerId) =>
  http.post(`/api/session/${sessionId}/leave`, null, {
    headers: { 'X-Player-Id': playerId },
  });

/** GET /api/session/{sessionId}/roster → Set<playerId> (serialised as array). */
export const getRoster = (sessionId) =>
  http.get(`/api/session/${sessionId}/roster`).then((r) => r.data);

