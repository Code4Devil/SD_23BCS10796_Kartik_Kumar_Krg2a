import { http } from './api';

/**
 * GET /api/anticheat/{sessionId}/stats — per-player aggregate cheat counters
 * for the host dashboard. Returns
 *   [{ playerId, counts: { TAB_HIDDEN: 2, WINDOW_BLUR: 1, ... }, totalSeverity: 12 }, ...]
 */
export const getCheatStats = (sessionId) =>
  http.get(`/api/anticheat/${sessionId}/stats`).then((r) => r.data);

/** GET /api/anticheat/{sessionId}/players/{playerId}/logs — raw event log. */
export const getPlayerCheatLogs = (sessionId, playerId) =>
  http
    .get(`/api/anticheat/${sessionId}/players/${playerId}/logs`)
    .then((r) => r.data);

