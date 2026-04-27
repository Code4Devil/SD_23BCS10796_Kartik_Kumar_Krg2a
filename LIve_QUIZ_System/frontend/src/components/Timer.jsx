export default function Timer({ seconds, total }) {
  const pct = total ? Math.max(0, Math.min(100, (seconds / total) * 100)) : 0;
  const danger = seconds <= 5;
  return (
    <div className="flex items-center gap-3 w-full">
      <div
        className={`text-4xl font-black tabular-nums ${
          danger ? 'text-brand-red animate-pulse' : ''
        }`}
      >
        {seconds}s
      </div>
      <div className="flex-1 h-3 rounded-full bg-white/20 overflow-hidden">
        <div
          className={`h-full transition-[width] duration-200 ${
            danger ? 'bg-brand-red' : 'bg-brand-yellow'
          }`}
          style={{ width: `${pct}%` }}
        />
      </div>
    </div>
  );
}

