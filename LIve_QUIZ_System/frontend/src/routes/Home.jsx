import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import { joinSession } from '../services/sessionApi';
import { registerDisplayName } from '../services/leaderboardApi';
import { useQuizStore } from '../store/useQuizStore';

/** 16-char opaque player id that survives page refreshes (see Zustand persist). */
function newPlayerId() {
  return 'p_' + Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4);
}

export default function Home() {
  const nav = useNavigate();
  const { user, setUser, setSessionId, resetSession } = useQuizStore();

  const [name, setName] = useState(user?.displayName ?? '');
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    setError(null);

    if (!name.trim()) return setError('Enter a display name');
    if (!code.trim()) return setError('Enter a room code');

    setBusy(true);
    try {
      const playerId = user?.playerId ?? newPlayerId();
      const sessionId = code.trim();

      await joinSession({ sessionId, playerId, displayName: name.trim() });
      // Best-effort: makes the leaderboard show the human name immediately.
      registerDisplayName(sessionId, playerId, name.trim()).catch(() => {});

      resetSession();
      setUser({ playerId, displayName: name.trim() });
      setSessionId(sessionId);
      nav('/waiting');
    } catch (err) {
      setError(err.message || 'Could not join that room');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="max-w-md mx-auto mt-12">
      <h1 className="text-4xl sm:text-5xl font-black text-center mb-2">
        Ready to play?
      </h1>
      <p className="text-center text-white/80 mb-8">
        Enter your name and the room code from your host.
      </p>

      <Card>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Display name
            </label>
            <input
              className="input"
              placeholder="e.g. Alex"
              value={name}
              onChange={(e) => setName(e.target.value)}
              maxLength={24}
              autoFocus
            />
          </div>
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Room code
            </label>
            <input
              className="input font-mono tracking-wider"
              placeholder="e.g. 3f9b-4d2a-..."
              value={code}
              onChange={(e) => setCode(e.target.value)}
            />
          </div>

          {error && (
            <p className="text-sm text-brand-red font-semibold">{error}</p>
          )}

          <Button type="submit" disabled={busy} className="w-full text-lg py-4">
            {busy ? 'Joining…' : 'Join quiz →'}
          </Button>
        </form>
      </Card>

      <p className="text-center text-white/60 text-xs mt-6">
        Hosts: create a quiz (POST&nbsp;<code>/api/quiz</code>) and start it
        (POST&nbsp;<code>/api/quiz/&#123;id&#125;/start</code>) — the returned
        session id is the room code.
      </p>
    </div>
  );
}

