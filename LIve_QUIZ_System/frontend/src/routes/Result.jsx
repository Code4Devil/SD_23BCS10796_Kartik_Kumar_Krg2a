import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import LeaderboardList from '../components/LeaderboardList';
import { useQuizStore } from '../store/useQuizStore';
import { getSessionResults } from '../services/quizApi';

export default function Result() {
  const nav = useNavigate();
  const { sessionId, user, leaderboard, resetSession } = useQuizStore();

  const [results, setResults] = useState(null);
  const [error, setError] = useState(null);

  // The durable Result table is written when QUIZ_ENDED is consumed by the
  // leaderboard service. We try a few times in case of replication lag.
  useEffect(() => {
    if (!sessionId) return;
    let alive = true;
    let attempts = 0;

    const tryLoad = async () => {
      attempts += 1;
      try {
        const data = await getSessionResults(sessionId);
        if (!alive) return;
        if (data?.length) {
          setResults(data);
        } else if (attempts < 5) {
          setTimeout(tryLoad, 1_500);
        } else {
          setResults([]);
        }
      } catch (e) {
        if (!alive) return;
        if (attempts < 5) setTimeout(tryLoad, 1_500);
        else setError(e.message);
      }
    };

    tryLoad();
    return () => { alive = false; };
  }, [sessionId]);

  // Derive "my result" from whichever source is populated first.
  const mine =
    results?.find((r) => r.playerId === user?.playerId) ||
    leaderboard.find((e) => e.playerId === user?.playerId);

  const playAgain = () => { resetSession(); nav('/'); };

  const finalBoard =
    results?.length
      ? results.map((r) => ({
          playerId: r.playerId,
          displayName: r.playerId,
          score: r.score,
          rank: r.rank,
        }))
      : leaderboard;

  return (
    <div className="max-w-2xl mx-auto mt-6 space-y-6">
      <Card className="text-center">
        <p className="text-sm uppercase tracking-wider text-slate-500">
          Quiz complete
        </p>
        <h1 className="text-4xl font-black mt-1">🎉 Nice one{user?.displayName ? `, ${user.displayName}` : ''}!</h1>
        {mine ? (
          <div className="mt-5 flex items-center justify-center gap-8">
            <div>
              <p className="text-xs uppercase text-slate-500">Your score</p>
              <p className="text-5xl font-black text-brand-purple">
                {Math.round(mine.score)}
              </p>
            </div>
            <div>
              <p className="text-xs uppercase text-slate-500">Your rank</p>
              <p className="text-5xl font-black text-brand-pink">#{mine.rank ?? '-'}</p>
            </div>
          </div>
        ) : (
          <p className="mt-4 text-slate-600">
            {error ? `Couldn't load results: ${error}` : 'Calculating final results…'}
          </p>
        )}
      </Card>

      <div>
        <h2 className="text-2xl font-black mb-3">Final leaderboard</h2>
        <LeaderboardList entries={finalBoard} currentPlayerId={user?.playerId} />
      </div>

      <div className="text-center">
        <Button onClick={playAgain}>Play another quiz</Button>
      </div>
    </div>
  );
}

