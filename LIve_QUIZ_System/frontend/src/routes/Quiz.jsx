import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import Card from '../components/Card';
import Button from '../components/Button';
import OptionCard from '../components/OptionCard';
import Timer from '../components/Timer';
import { useQuizStore } from '../store/useQuizStore';
import { useTimer } from '../hooks/useTimer';
import { useAntiCheat } from '../hooks/useAntiCheat';
import { ws } from '../services/ws';

export default function Quiz() {
  const nav = useNavigate();
  const {
    sessionId,
    user,
    currentQuestion,
    answeredFor,
    markAnswered,
    quizEnded,
  } = useQuizStore();

  useAntiCheat({ sessionId, playerId: user?.playerId, enabled: true });

  const [selected, setSelected] = useState(null);
  const [isFullscreen, setIsFullscreen] = useState(
    typeof document !== 'undefined' && !!document.fullscreenElement
  );

  // Track fullscreen state so we can gate the quiz UI behind it and prompt the
  // player to re-enter if they bail out mid-quiz. The FULLSCREEN_EXIT signal
  // itself is emitted by useAntiCheat — this effect is purely UI state.
  useEffect(() => {
    const onChange = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener('fullscreenchange', onChange);
    return () => document.removeEventListener('fullscreenchange', onChange);
  }, []);

  const enterFullscreen = async () => {
    try {
      await document.documentElement.requestFullscreen();
    } catch {
      // Browser refused (e.g. user denied gesture). Leave isFullscreen false.
    }
  };

  // Reset local selection whenever a new question is pushed.
  useEffect(() => {
    setSelected(null);
  }, [currentQuestion?.questionId]);

  const alreadyAnswered = currentQuestion
    ? answeredFor.includes(currentQuestion.questionId)
    : false;

  const remaining = useTimer(
    currentQuestion?.publishedAt,
    currentQuestion?.timeLimitSeconds
  );
  const locked = alreadyAnswered || remaining <= 0;

  // Auto-route to the result screen when the backend signals end of quiz.
  // quizEnded is flipped by useSessionWire subscribing to
  // /topic/session/{sid}/ended, which the orchestrator emits once all
  // questions have been exhausted.
  useEffect(() => {
    if (quizEnded) nav('/result');
  }, [quizEnded, nav]);

  if (!currentQuestion) {
    return (
      <div className="max-w-xl mx-auto mt-16 text-center">
        <Card>
          <p className="text-lg">Waiting for the next question…</p>
        </Card>
      </div>
    );
  }

  // Fullscreen gate — browsers only grant requestFullscreen from a user
  // gesture, so we present a blocking CTA instead of auto-requesting. The
  // same screen reappears if the player exits fullscreen mid-quiz.
  if (!isFullscreen) {
    return (
      <div className="max-w-xl mx-auto mt-16 text-center">
        <Card className="space-y-4">
          <h2 className="text-2xl font-black">🖥 Fullscreen required</h2>
          <p className="text-slate-700">
            To keep the quiz fair, please play in fullscreen mode. Leaving
            fullscreen is logged and counts against your anti-cheat score.
          </p>
          <Button onClick={enterFullscreen} className="text-lg px-8 py-4">
            Enter fullscreen & continue
          </Button>
        </Card>
      </div>
    );
  }

  const choose = (i) => {
    if (locked) return;
    setSelected(i);
    ws.sendAnswer({
      sessionId,
      playerId: user.playerId,
      questionId: currentQuestion.questionId,
      selectedOptionIndex: i,
    });
    markAnswered(currentQuestion.questionId);
  };

  return (
    <div className="max-w-3xl mx-auto mt-4 space-y-5">
      <div className="flex items-center justify-between text-sm">
        <span className="px-3 py-1 rounded-full bg-white/15">
          Question #{currentQuestion.questionIndex + 1}
        </span>
        <button
          className="underline text-white/80"
          onClick={() => nav('/leaderboard')}
        >
          Show leaderboard →
        </button>
      </div>

      <AnimatePresence mode="wait">
        <motion.div
          key={currentQuestion.questionId}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -12 }}
          transition={{ duration: 0.25 }}
        >
          <Card className="mb-4">
            <h2 className="text-2xl sm:text-3xl font-black mb-5 leading-snug">
              {currentQuestion.text}
            </h2>
            <Timer
              seconds={remaining}
              total={currentQuestion.timeLimitSeconds}
            />
          </Card>
        </motion.div>
      </AnimatePresence>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {currentQuestion.options.map((opt, i) => (
          <OptionCard
            key={i}
            index={i}
            text={opt}
            selected={selected === i}
            disabled={locked && selected !== i}
            onClick={() => choose(i)}
          />
        ))}
      </div>

      <div className="text-center text-white/80 text-sm pt-2">
        {alreadyAnswered
          ? '✅ Answer locked in — waiting for the next question'
          : remaining <= 0
            ? '⏰ Time\'s up!'
            : 'Pick the correct answer before the timer runs out'}
      </div>
    </div>
  );
}

