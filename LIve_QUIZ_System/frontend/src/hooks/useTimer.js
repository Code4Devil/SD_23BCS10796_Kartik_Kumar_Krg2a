import { useEffect, useState } from 'react';

/**
 * Server-clock-anchored countdown.
 *
 * The backend includes `publishedAt` (ISO instant) on every QuestionPublishedEvent
 * precisely so all clients can compute a consistent remaining time regardless
 * of their local clock drift.
 *
 *   remaining = timeLimitSeconds - (now - publishedAt)
 *
 * Ticks every 200ms for smooth UI without spamming re-renders.
 */
export function useTimer(publishedAt, timeLimitSeconds) {
  const computeRemaining = () => {
    if (!publishedAt || !timeLimitSeconds) return 0;
    const started = new Date(publishedAt).getTime();
    const elapsed = (Date.now() - started) / 1000;
    return Math.max(0, Math.ceil(timeLimitSeconds - elapsed));
  };

  const [remaining, setRemaining] = useState(computeRemaining);

  useEffect(() => {
    setRemaining(computeRemaining());
    const id = setInterval(() => setRemaining(computeRemaining()), 200);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [publishedAt, timeLimitSeconds]);

  return remaining;
}

