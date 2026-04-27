import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import PlayerList from '../components/PlayerList';
import { useQuizStore } from '../store/useQuizStore';
import { getRoster } from '../services/sessionApi';

export default function Waiting() {
  const nav = useNavigate();
  const { sessionId, user, roster, setRoster, currentQuestion } = useQuizStore();
  const [copied, setCopied] = useState(false);

  // Poll roster every 3s (lightweight; server is Redis-backed). A dedicated
  // /topic/session/{id}/roster topic could replace this later.
  useEffect(() => {
    if (!sessionId) return;
    let alive = true;
    const load = async () => {
      try {
        const r = await getRoster(sessionId);
        if (alive) setRoster(Array.isArray(r) ? r : Array.from(r));
      } catch { /* transient — keep showing last roster */ }
    };
    load();
    const id = setInterval(load, 3_000);
    return () => { alive = false; clearInterval(id); };
  }, [sessionId, setRoster]);

  // As soon as the orchestrator publishes the first question, move to the
  // quiz screen. This is the "host pressed start" trigger for players.
  useEffect(() => {
    if (currentQuestion) nav('/quiz');
  }, [currentQuestion, nav]);

  const copyCode = async () => {
    try {
      await navigator.clipboard.writeText(sessionId);
      setCopied(true);
      setTimeout(() => setCopied(false), 1_500);
    } catch { /* ignore */ }
  };

  return (
    <div className="max-w-2xl mx-auto mt-6 space-y-6">
      <Card>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-wider text-slate-500">
              Room code
            </p>
            <p className="text-2xl font-black font-mono break-all">{sessionId}</p>
          </div>
          <button
            onClick={copyCode}
            className="px-4 py-2 rounded-xl bg-slate-900 text-white text-sm font-semibold"
          >
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>
      </Card>

      <div>
        <div className="flex items-baseline justify-between mb-3">
          <h2 className="text-2xl font-black">Players in the room</h2>
          <span className="text-sm text-white/80">{roster.length} joined</span>
        </div>
        {roster.length === 0 ? (
          <p className="text-white/70">Waiting for players to join…</p>
        ) : (
          <PlayerList players={roster} currentPlayerId={user?.playerId} />
        )}
      </div>

      <div className="text-center text-white/70 text-sm">
        ⏳ Waiting for the host to start the quiz — you'll jump straight to the
        first question when it does.
      </div>
    </div>
  );
}

