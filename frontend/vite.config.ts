import { env } from 'node:process';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    proxy: {
      '/actuator/health/readiness': {
        target: env.BACKEND_URL ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
