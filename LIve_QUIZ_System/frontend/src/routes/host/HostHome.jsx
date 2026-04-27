import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../../components/Card';
import Button from '../../components/Button';
import { useHostStore } from '../../store/useHostStore';

function randomHostId() {
  return 'host_' + Math.random().toString(36).slice(2, 10);
}

export default function HostHome() {
  const nav = useNavigate();
  const { hostId, setHostId, quizId, sessionId, reset } = useHostStore();

  const [name, setName] = useState(hostId ?? '');

  useEffect(() => {
    if (!hostId) {
      const id = randomHostId();
      setHostId(id);
      setName(id);
    }
  }, [hostId, setHostId]);

  const save = () => {
    const id = name.trim() || randomHostId();
    setHostId(id);
  };

  return (
    <div className="max-w-xl mx-auto mt-10 space-y-6">
      <h1 className="text-4xl font-black text-center">🎤 Host dashboard</h1>
      <p className="text-center text-white/80">
        Create a quiz, start a live session, and run the show.
      </p>

      <Card className="space-y-4">
        <div>
          <label className="block text-sm font-semibold text-slate-700 mb-1">
            Host id
          </label>
          <div className="flex gap-2">
            <input
              className="input font-mono"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
            <Button variant="ghost" className="text-slate-900" onClick={save}>
              Save
            </Button>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Used as <code>X-Player-Id</code> on the start call.
          </p>
        </div>
      </Card>

      <div className="grid gap-3 sm:grid-cols-2">
        <Button onClick={() => nav('/host/create')} className="py-5 text-lg">
          ➕ Create a new quiz
        </Button>
        <Button
          variant="ghost"
          className="py-5 text-lg"
          disabled={!quizId}
          onClick={() => nav(`/host/quiz/${quizId}`)}
        >
          📋 Last quiz {quizId ? `(${quizId.slice(0, 6)}…)` : '(none)'}
        </Button>
      </div>

      {sessionId && (
        <Card className="flex items-center justify-between">
          <div>
            <p className="text-xs uppercase text-slate-500">Active session</p>
            <p className="font-mono break-all">{sessionId}</p>
          </div>
          <div className="flex gap-2">
            <Button onClick={() => nav(`/host/session/${sessionId}`)}>Rejoin</Button>
            <Button
              variant="ghost"
              className="text-slate-900"
              onClick={() => reset()}
            >
              Clear
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}

