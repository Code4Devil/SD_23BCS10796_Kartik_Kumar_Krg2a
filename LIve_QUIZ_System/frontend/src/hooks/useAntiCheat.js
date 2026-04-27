import { useEffect } from 'react';
import { ws } from '../services/ws';

/**
 * Wires up browser-level anti-cheat signals and forwards them over STOMP.
 *
 * Signals mirror the backend's {@code BehaviorEvent.BehaviorType} enum:
 *   visibilitychange (hidden) -> TAB_HIDDEN
 *   visibilitychange (shown)  -> TAB_VISIBLE
 *   window blur                -> WINDOW_BLUR
 *   window focus               -> WINDOW_FOCUS
 *   copy                       -> COPY
 *   paste                      -> PASTE
 *   fullscreenchange (exit)    -> FULLSCREEN_EXIT
 *
 * `enabled` lets pages opt in (only the quiz page really needs it).
 */
export function useAntiCheat({ sessionId, playerId, enabled = true }) {
  useEffect(() => {
    if (!enabled || !sessionId || !playerId) return;

    const emit = (type) => ws.sendBehavior({ sessionId, playerId, type });

    const onVis = () => emit(document.hidden ? 'TAB_HIDDEN' : 'TAB_VISIBLE');
    const onBlur = () => emit('WINDOW_BLUR');
    const onFocus = () => emit('WINDOW_FOCUS');
    const onCopy = () => emit('COPY');
    const onPaste = () => emit('PASTE');
    const onFsChange = () => {
      if (!document.fullscreenElement) emit('FULLSCREEN_EXIT');
    };

    document.addEventListener('visibilitychange', onVis);
    window.addEventListener('blur', onBlur);
    window.addEventListener('focus', onFocus);
    document.addEventListener('copy', onCopy);
    document.addEventListener('paste', onPaste);
    document.addEventListener('fullscreenchange', onFsChange);

    return () => {
      document.removeEventListener('visibilitychange', onVis);
      window.removeEventListener('blur', onBlur);
      window.removeEventListener('focus', onFocus);
      document.removeEventListener('copy', onCopy);
      document.removeEventListener('paste', onPaste);
      document.removeEventListener('fullscreenchange', onFsChange);
    };
  }, [sessionId, playerId, enabled]);
}

