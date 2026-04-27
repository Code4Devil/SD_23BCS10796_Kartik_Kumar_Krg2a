import { http } from './api';

/**
 * REST fallback for answer submission. The preferred hot path is the
 * /app/answer STOMP frame (see services/ws.js), but we keep a REST shim for
 * clients that cannot hold a WS open.
 */
export const submitAnswerRest = (dto) =>
  http.post('/api/answer/submit', dto).then((r) => r.data);

