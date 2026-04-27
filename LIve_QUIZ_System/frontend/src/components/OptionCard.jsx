import { motion } from 'framer-motion';

// Kahoot-style bold colours, 1 per option index (wraps after 4).
const SKIN = [
  { bg: 'bg-brand-red',    shape: '▲' },
  { bg: 'bg-brand-blue',   shape: '◆' },
  { bg: 'bg-brand-yellow text-slate-900', shape: '●' },
  { bg: 'bg-brand-green',  shape: '■' },
];

export default function OptionCard({ index, text, selected, disabled, onClick }) {
  const skin = SKIN[index % SKIN.length];
  return (
    <motion.button
      whileTap={{ scale: 0.97 }}
      disabled={disabled}
      onClick={onClick}
      className={`
        ${skin.bg} text-white text-left px-5 py-6 rounded-2xl font-bold text-lg
        shadow-pop w-full flex items-center gap-4 transition
        ${selected ? 'ring-4 ring-white' : 'hover:brightness-110'}
        ${disabled ? 'opacity-60 cursor-not-allowed' : ''}
      `}
    >
      <span className="text-2xl w-10 text-center">{skin.shape}</span>
      <span className="flex-1">{text}</span>
    </motion.button>
  );
}

