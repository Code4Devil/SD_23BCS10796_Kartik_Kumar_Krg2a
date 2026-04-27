import { motion, AnimatePresence } from 'framer-motion';

const MEDAL = ['🥇', '🥈', '🥉'];

export default function LeaderboardList({ entries = [], currentPlayerId }) {
  if (!entries.length) {
    return (
      <p className="text-center text-white/70 py-8">No scores yet — be the first!</p>
    );
  }
  return (
    <ul className="space-y-2">
      <AnimatePresence initial={false}>
        {entries.map((e) => {
          const you = e.playerId === currentPlayerId;
          return (
            <motion.li
              key={e.playerId}
              layout
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0 }}
              transition={{ type: 'spring', stiffness: 400, damping: 30 }}
              className={`
                flex items-center gap-3 px-4 py-3 rounded-xl font-semibold
                ${you
                  ? 'bg-brand-yellow text-slate-900 ring-4 ring-white/60'
                  : 'bg-white/15 text-white'}
              `}
            >
              <span className="w-10 text-center text-xl">
                {MEDAL[e.rank - 1] ?? `#${e.rank}`}
              </span>
              <span className="flex-1 truncate">
                {e.displayName || e.playerId.slice(0, 8)}
                {you && <span className="ml-2 text-xs opacity-80">(you)</span>}
              </span>
              <span className="tabular-nums font-black">{Math.round(e.score)}</span>
            </motion.li>
          );
        })}
      </AnimatePresence>
    </ul>
  );
}

