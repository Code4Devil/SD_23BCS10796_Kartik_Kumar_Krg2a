import axios from 'axios';

// `??` (not `||`) so an empty string in .env is honoured and axios falls
// back to same-origin relative URLs — which the Vite dev proxy forwards
// to the api-gateway, sidestepping CORS entirely.
const BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';

/**
 * Central axios instance.
 *
 * Auth: if VITE_JWT_TOKEN is set (or a token has been stashed under
 * `livequiz:jwt` in localStorage), we attach it as Bearer. When the backend
 * gateway is bypassed in dev, no token is needed.
 */
export const http = axios.create({
  baseURL: BASE,
  timeout: 10_000,
  headers: { 'Content-Type': 'application/json' },
});

http.interceptors.request.use((config) => {
  const token =
    localStorage.getItem('livequiz:jwt') || import.meta.env.VITE_JWT_TOKEN || '';
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (r) => r,
  (err) => {
    // Bubble a friendlier error for the UI layer.
    const msg =
      err.response?.data?.message ||
      err.response?.statusText ||
      err.message ||
      'Network error';
    return Promise.reject(new Error(msg));
  }
);

