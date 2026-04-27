import { Outlet, Link, useLocation } from 'react-router-dom';
import { useQuizStore } from '../store/useQuizStore';

export default function Layout() {
  const { user, sessionId, connected, cheatAlert, setCheatAlert } = useQuizStore();
  const { pathname } = useLocation();
  const inHost = pathname.startsWith('/host');

  return (
    <div className="min-h-screen flex flex-col">
      <header className="px-5 py-4 flex items-center justify-between">
        <Link to="/" className="text-xl font-black tracking-tight">
          ⚡ LiveQuiz
        </Link>
        <div className="flex items-center gap-3 text-sm">
          <Link
            to={inHost ? '/' : '/host'}
            className="px-3 py-1 rounded-full bg-white/15 hover:bg-white/25"
          >
            {inHost ? 'Player mode' : 'Host mode →'}
          </Link>
          {user?.displayName && !inHost && (
            <span className="px-3 py-1 rounded-full bg-white/15">
              {user.displayName}
            </span>
          )}
          {sessionId && !inHost && (
            <span className="px-3 py-1 rounded-full bg-white/15 font-mono">
              {sessionId.slice(0, 8)}
            </span>
          )}
          {!inHost && (
            <span
              className={`w-2.5 h-2.5 rounded-full ${
                connected ? 'bg-brand-green' : 'bg-brand-red'
              }`}
              title={connected ? 'Live' : 'Offline'}
            />
          )}
        </div>
      </header>

      <main className="flex-1 px-4 pb-8">
        <Outlet />
      </main>

      {cheatAlert && (
        <div
          role="alert"
          className="fixed bottom-4 left-1/2 -translate-x-1/2 bg-brand-red/95
                     text-white px-5 py-3 rounded-xl shadow-xl max-w-md text-sm
                     flex items-center gap-3"
        >
          <span>⚠️ {cheatAlert}</span>
          <button
            className="underline text-white/80"
            onClick={() => setCheatAlert(null)}
          >
            dismiss
          </button>
        </div>
      )}
    </div>
  );
}

