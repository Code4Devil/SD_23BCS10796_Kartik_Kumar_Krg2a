/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          purple: '#7c3aed',
          pink: '#ec4899',
          yellow: '#fde047',
          green: '#22c55e',
          red: '#ef4444',
          blue: '#3b82f6',
        },
      },
      fontFamily: {
        display: ['"Inter"', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        pop: '0 10px 0 0 rgba(0,0,0,0.25)',
      },
      keyframes: {
        pop: { '0%': { transform: 'scale(0.95)' }, '100%': { transform: 'scale(1)' } },
      },
      animation: { pop: 'pop 150ms ease-out' },
    },
  },
  plugins: [],
};

