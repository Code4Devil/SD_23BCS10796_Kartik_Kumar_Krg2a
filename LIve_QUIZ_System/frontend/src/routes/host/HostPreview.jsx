import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Card from '../../components/Card';
import Button from '../../components/Button';
import { useHostStore } from '../../store/useHostStore';
import { getQuiz, startQuiz } from '../../services/quizApi';

/**
 * Review a just-created quiz and open the live lobby.
 *
 * `POST /api/quiz/{id}/start` opens the room in CREATED state — no question is
 * published yet. The host shares the room code, waits for players, then hits
 * "Start quiz" on the session page to publish the first question.
 */
export default function HostPreview() {
  const { quizId } = useParams();
  const nav = useNavigate();
  const { hostId, setSessionId } = useHostStore();

  const [quiz, setQuiz] = useState(null);
  const [error, setError] = useState(null);
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    if (!quizId) return;
    getQuiz(quizId)
      .then(setQuiz)
      .catch((e) => setError(e.message));
  }, [quizId]);

  const start = async () => {
    setStarting(true);
    setError(null);
    try {
      const session = await startQuiz(quizId, hostId ?? 'host');
      setSessionId(session.id);
      nav(`/host/session/${session.id}`);
    } catch (e) {
      setError(e.message);
    } finally {
      setStarting(false);
    }
  };

  if (error) {
    return (
      <Card className="max-w-xl mx-auto mt-10 text-center">
        <p className="text-brand-red font-semibold">{error}</p>
        <Button className="mt-4" onClick={() => nav('/host')}>← Back</Button>
      </Card>
    );
  }

  if (!quiz) {
    return <p className="text-center mt-10 text-white/80">Loading quiz…</p>;
  }

  return (
    <div className="max-w-3xl mx-auto mt-4 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-wider text-white/70">Quiz</p>
          <h1 className="text-3xl font-black">{quiz.title}</h1>
          <p className="text-white/70 text-sm font-mono">{quiz.id}</p>
        </div>
        <Button variant="ghost" onClick={() => nav('/host')}>← Home</Button>
      </div>

      <Card className="bg-yellow-50 border-l-4 border-amber-400 text-slate-800">
        <p className="font-semibold">💡 How it works</p>
        <p className="text-sm">
          Pressing <b>Start live session</b> opens the room in lobby mode — no
          questions are published yet. Share the room code with your players,
          wait for them to join, then press <b>Start quiz</b> on the next screen
          to publish question 1.
        </p>
      </Card>

      <div className="space-y-3">
        {quiz.questions?.map((q, i) => (
          <Card key={q.id ?? i}>
            <div className="flex items-start justify-between mb-2">
              <h3 className="font-black">
                Q{i + 1}. {q.text}
              </h3>
              <span className="text-xs font-mono px-2 py-1 bg-slate-100 rounded">
                {q.timeLimitSeconds}s
              </span>
            </div>
            <ul className="grid sm:grid-cols-2 gap-2">
              {q.options.map((o, oi) => (
                <li
                  key={oi}
                  className={`px-3 py-2 rounded-lg text-sm ${
                    oi === q.correctOptionIndex
                      ? 'bg-brand-green/15 text-green-800 font-semibold'
                      : 'bg-slate-100 text-slate-700'
                  }`}
                >
                  {oi === q.correctOptionIndex && '✓ '}
                  {o}
                </li>
              ))}
            </ul>
          </Card>
        ))}
      </div>

      <div className="flex justify-end">
        <Button onClick={start} disabled={starting} className="text-lg px-8 py-4">
          {starting ? 'Opening lobby…' : '▶ Open lobby'}
        </Button>
      </div>
    </div>
  );
}

