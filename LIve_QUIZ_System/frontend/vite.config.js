import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// SockJS relies on `global` being defined in the browser.
export default defineConfig({
  plugins: [react()],
  define: { global: 'globalThis' },
  server: {
    port: 5173,
    host: true,
    // Dev proxy avoids CORS: everything is same-origin on :5173 and Vite
    // forwards /api and /ws to the api-gateway on :8080.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
});

