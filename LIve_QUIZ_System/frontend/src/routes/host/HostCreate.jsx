import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Card from '../../components/Card';
import Button from '../../components/Button';
import { useHostStore } from '../../store/useHostStore';
import { createQuiz } from '../../services/quizApi';

const blankQuestion = () => ({
  text: '',
  options: ['', '', '', ''],
  correctOptionIndex: 0,
  timeLimitSeconds: 20,
});

const SAMPLE = {
  title: 'General Knowledge Demo',
  questions: [
    {
      text: 'Which planet is known as the Red Planet?',
      options: ['Venus', 'Mars', 'Jupiter', 'Saturn'],
      correctOptionIndex: 1,
      timeLimitSeconds: 15,
    },
    {
      text: 'What is 12 × 12?',
      options: ['124', '132', '144', '156'],
      correctOptionIndex: 2,
      timeLimitSeconds: 10,
    },
  ],
};

export default function HostCreate() {
  const nav = useNavigate();
  const { hostId, setQuizId } = useHostStore();

  const [title, setTitle] = useState('');
  const [questions, setQuestions] = useState([blankQuestion()]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const patchQ = (i, patch) =>
    setQuestions((qs) => qs.map((q, idx) => (idx === i ? { ...q, ...patch } : q)));
  const patchOpt = (qi, oi, val) =>
    patchQ(qi, { options: questions[qi].options.map((o, j) => (j === oi ? val : o)) });

  const addQ = () => setQuestions((qs) => [...qs, blankQuestion()]);
  const removeQ = (i) =>
    setQuestions((qs) => (qs.length > 1 ? qs.filter((_, idx) => idx !== i) : qs));

  const loadSample = () => {
    setTitle(SAMPLE.title);
    setQuestions(SAMPLE.questions.map((q) => ({ ...q, options: [...q.options] })));
  };

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    if (!title.trim()) return setError('Title is required');
    for (const [i, q] of questions.entries()) {
      if (!q.text.trim()) return setError(`Question ${i + 1}: text is required`);
      if (q.options.some((o) => !o.trim()))
        return setError(`Question ${i + 1}: all options must be filled`);
    }

    setBusy(true);
    try {
      const created = await createQuiz({
        title: title.trim(),
        hostId: hostId ?? 'host',
        questions,
      });
      setQuizId(created.id);
      nav(`/host/quiz/${created.id}`);
    } catch (err) {
      setError(err.message || 'Could not create quiz');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto mt-4 space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-black">📝 New quiz</h1>
        <Button variant="ghost" onClick={loadSample}>Load sample</Button>
      </div>

      <form onSubmit={submit} className="space-y-5">
        <Card>
          <label className="block text-sm font-semibold text-slate-700 mb-1">Title</label>
          <input
            className="input"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Friday Trivia Night"
          />
        </Card>

        {questions.map((q, qi) => (
          <Card key={qi} className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-black text-lg">Question {qi + 1}</h3>
              <button
                type="button"
                className="text-sm text-brand-red font-semibold disabled:opacity-40"
                onClick={() => removeQ(qi)}
                disabled={questions.length === 1}
              >
                Remove
              </button>
            </div>

            <input
              className="input"
              placeholder="Question text"
              value={q.text}
              onChange={(e) => patchQ(qi, { text: e.target.value })}
            />

            <div className="grid sm:grid-cols-2 gap-2">
              {q.options.map((opt, oi) => (
                <label key={oi} className="flex items-center gap-2">
                  <input
                    type="radio"
                    name={`correct-${qi}`}
                    checked={q.correctOptionIndex === oi}
                    onChange={() => patchQ(qi, { correctOptionIndex: oi })}
                  />
                  <input
                    className="input flex-1"
                    placeholder={`Option ${oi + 1}`}
                    value={opt}
                    onChange={(e) => patchOpt(qi, oi, e.target.value)}
                  />
                </label>
              ))}
            </div>

            <div className="flex items-center gap-2">
              <label className="text-sm font-semibold text-slate-700">
                Time limit (s)
              </label>
              <input
                type="number" min={5} max={300}
                className="input w-24"
                value={q.timeLimitSeconds}
                onChange={(e) =>
                  patchQ(qi, { timeLimitSeconds: Number(e.target.value) || 0 })}
              />
              <span className="text-xs text-slate-500 ml-auto">
                Radio = correct answer
              </span>
            </div>
          </Card>
        ))}

        {error && <p className="text-brand-red font-semibold">{error}</p>}

        <div className="flex flex-wrap gap-3">
          <Button type="button" variant="ghost" onClick={addQ}>+ Add question</Button>
          <Button type="submit" disabled={busy} className="ml-auto">
            {busy ? 'Creating…' : 'Create quiz →'}
          </Button>
        </div>
      </form>
    </div>
  );
}

