import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Button from '../components/Button';
import LeaderboardList from '../components/LeaderboardList';
import { useQuizStore } from '../store/useQuizStore';
import { getLeaderboardTop } from '../services/leaderboardApi';

export default function Leaderboard() {
  const nav = useNavigate();
  const { sessionId, user, leaderboard, setLeaderboard, currentQuestion } =
    useQuizStore();

  // Seed from a REST snapshot; subsequent updates stream over WS.
  useEffect(() => {
    if (!sessionId) return;
    getLeaderboardTop(sessionId)
      .then((entries) => Array.isArray(entries) && setLeaderboard(entries))
      .catch(() => {});
  }, [sessionId, setLeaderboard]);

  return (
    <div className="max-w-2xl mx-auto mt-4 space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-black">🏆 Live leaderboard</h1>
        {currentQuestion && (
          <Button variant="ghost" onClick={() => nav('/quiz')}>
            ← Back to question
          </Button>
        )}
      </div>

      <Card className="bg-transparent shadow-none p-0">
        <LeaderboardList
          entries={leaderboard}
          currentPlayerId={user?.playerId}
        />
      </Card>
    </div>
  );
}

