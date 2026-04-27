import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import Home from './routes/Home';
import Waiting from './routes/Waiting';
import Quiz from './routes/Quiz';
import Leaderboard from './routes/Leaderboard';
import Result from './routes/Result';
import HostHome from './routes/host/HostHome';
import HostCreate from './routes/host/HostCreate';
import HostPreview from './routes/host/HostPreview';
import HostSession from './routes/host/HostSession';
import { useQuizStore } from './store/useQuizStore';
import { useSessionWire } from './hooks/useSessionWire';

/**
 * A single guard that mounts the session WS wire whenever a user+sessionId
 * exists. Children routes get live question/leaderboard updates via Zustand.
 */
function SessionGuard({ children }) {
  const { user, sessionId } = useQuizStore();
  useSessionWire();
  if (!user?.playerId || !sessionId) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route
          path="/waiting"
          element={
            <SessionGuard>
              <Waiting />
            </SessionGuard>
          }
        />
        <Route
          path="/quiz"
          element={
            <SessionGuard>
              <Quiz />
            </SessionGuard>
          }
        />
        <Route
          path="/leaderboard"
          element={
            <SessionGuard>
              <Leaderboard />
            </SessionGuard>
          }
        />
        <Route
          path="/result"
          element={
            <SessionGuard>
              <Result />
            </SessionGuard>
          }
        />

        {/* Host routes. Deliberately not behind SessionGuard — hosts don't
            carry a player session, they create and observe one. */}
        <Route path="/host" element={<HostHome />} />
        <Route path="/host/create" element={<HostCreate />} />
        <Route path="/host/quiz/:quizId" element={<HostPreview />} />
        <Route path="/host/session/:sessionId" element={<HostSession />} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}

