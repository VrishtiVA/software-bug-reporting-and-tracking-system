import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * Useful documentation: https://vite.dev/config/
 */
export default defineConfig({
  plugins: [react()],
  //Set up dev server proxy rules for forwarding requests to backend.
  server: {
    proxy: {
      "/api": "http://localhost:8080",
      "/notifications/stream": {
        target: "http://localhost:8080",
        ws: true //Allow websocket type requests to be forwarded to backend
      }
    }
  }
})
