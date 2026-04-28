import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  // JAR에 넣어 같은 호스트(8080)에서 서빙할 때 루트 기준
  base: "/",
  plugins: [react()],
  server: {
    port: 5173,
    headers: {
      "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
      Pragma: "no-cache",
      Expires: "0",
    },
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
