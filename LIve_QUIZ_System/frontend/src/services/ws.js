import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Accept both absolute ("http://host:port/ws") and relative ("/ws") forms.
// Relative is preferred in dev so the Vite proxy handles the upgrade and
// we avoid CORS. SockJS requires an absolute URL, so we resolve against
// window.location when a relative path is supplied.
const RAW_WS = import.meta.env.VITE_WS_URL ?? 'http://localhost:8080/ws';
const WS_URL =
  RAW_WS.startsWith('http') || RAW_WS.startsWith('ws')
    ? RAW_WS
    : `${window.location.origin}${RAW_WS.startsWith('/') ? '' : '/'}${RAW_WS}`;

/**
 * Thin wrapper around a single STOMP-over-SockJS client.
 *
 * Design:
 *   - One connection per browser tab, shared by every route.
 *   - `connect()` is idempotent; routes call it on mount.
 *   - Topic subscriptions are keyed by sessionId so we can tear them down
 *     on leave without affecting the connection.
 *   - Automatic reconnect is handled by @stomp/stompjs (reconnectDelay).
 */
class QuizSocket {
  constructor() {
    this.client = null;
    this.subs = new Map(); // destination -> StompSubscription
    this.onConnectCbs = [];
    this.onDisconnectCbs = [];
  }

  connect() {
    if (this.client && (this.client.active || this.client.connected)) return;

    this.client = new Client({
      // SockJS factory — survives strict proxies that block raw WS.
      webSocketFactory: () => new SockJS(WS_URL),
      reconnectDelay: 3_000,
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      debug: () => {}, // quiet; flip to console.log for debugging
      onConnect: () => this.onConnectCbs.forEach((cb) => cb()),
      onWebSocketClose: () => this.onDisconnectCbs.forEach((cb) => cb()),
      onStompError: (f) => console.error('STOMP error', f.headers['message'], f.body),
    });
    this.client.activate();
  }

  disconnect() {
    this.subs.forEach((s) => s.unsubscribe());
    this.subs.clear();
    if (this.client) this.client.deactivate();
    this.client = null;
  }

  onConnect(cb) {
    this.onConnectCbs.push(cb);
    if (this.client?.connected) cb();
    return () => {
      this.onConnectCbs = this.onConnectCbs.filter((c) => c !== cb);
    };
  }

  onDisconnect(cb) {
    this.onDisconnectCbs.push(cb);
    return () => {
      this.onDisconnectCbs = this.onDisconnectCbs.filter((c) => c !== cb);
    };
  }

  /** Subscribe with auto-JSON decode. Returns an unsubscribe function. */
  subscribe(destination, handler) {
    const doSubscribe = () => {
      if (this.subs.has(destination)) return;
      const sub = this.client.subscribe(destination, (frame) => {
        try {
          handler(frame.body ? JSON.parse(frame.body) : null);
        } catch {
          handler(frame.body);
        }
      });
      this.subs.set(destination, sub);
    };

    if (this.client?.connected) {
      doSubscribe();
    } else {
      // Defer until connected.
      this.onConnect(doSubscribe);
    }

    return () => {
      const s = this.subs.get(destination);
      if (s) {
        s.unsubscribe();
        this.subs.delete(destination);
      }
    };
  }

  /** Send a JSON body to an /app destination. Buffered until connected. */
  send(destination, body) {
    const payload = JSON.stringify(body);
    const publish = () =>
      this.client.publish({ destination, body: payload });
    if (this.client?.connected) publish();
    else this.onConnect(publish);
  }

  // Session-scoped topic helpers -----------------------------------------

  subscribeQuestion(sessionId, handler) {
    return this.subscribe(`/topic/session/${sessionId}/question`, handler);
  }
  subscribeLeaderboard(sessionId, handler) {
    return this.subscribe(`/topic/session/${sessionId}/leaderboard`, handler);
  }
  subscribeCheatAlert(sessionId, handler) {
    return this.subscribe(`/topic/session/${sessionId}/cheat-alert`, handler);
  }
  subscribeQuizEnded(sessionId, handler) {
    return this.subscribe(`/topic/session/${sessionId}/ended`, handler);
  }

  sendAnswer({ sessionId, playerId, questionId, selectedOptionIndex }) {
    this.send('/app/answer', { sessionId, playerId, questionId, selectedOptionIndex });
  }

  sendBehavior({ sessionId, playerId, type }) {
    this.send('/app/behavior', {
      sessionId,
      playerId,
      type,
      userAgent: navigator.userAgent,
      ipAddress: '', // server-filled
      occurredAt: new Date().toISOString(),
    });
  }
}

export const ws = new QuizSocket();

