import { motion, AnimatePresence } from 'framer-motion';

export default function PlayerList({ players = [], currentPlayerId }) {
  return (
    <ul className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
      <AnimatePresence initial={false}>
        {players.map((p) => (
          <motion.li
            key={p}
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9 }}
            className={`
              px-4 py-3 rounded-xl text-center font-semibold truncate
              ${p === currentPlayerId
                ? 'bg-brand-yellow text-slate-900'
                : 'bg-white/15'}
            `}
            title={p}
          >
            {p === currentPlayerId ? 'You' : p.slice(0, 8)}
          </motion.li>
        ))}
      </AnimatePresence>
    </ul>
  );
}

