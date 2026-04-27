import { http } from './api';

/** GET /api/leaderboard/{sessionId}/top — snapshot for initial page load. */
export const getLeaderboardTop = (sessionId) =>
  http.get(`/api/leaderboard/${sessionId}/top`).then((r) => r.data);

/** POST /api/leaderboard/{sessionId}/names?playerId=..&displayName=.. */
export const registerDisplayName = (sessionId, playerId, displayName) =>
  http.post(
    `/api/leaderboard/${sessionId}/names`,
    null,
    { params: { playerId, displayName } }
  );

