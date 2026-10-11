import { defineConfig, devices } from "@playwright/test";

// Dedicated ports so a running dev stack (8080 / 5173) is never touched.
const BACKEND_PORT = 8081;
const FRONTEND_PORT = 5174;

export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: [["html", { open: "never" }], ["list"]],
  use: {
    // The Vite dev server proxies /api to the e2e backend, so one origin serves page and API.
    baseURL: `http://localhost:${FRONTEND_PORT}`,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 7"] } },
  ],
  webServer: [
    {
      // Needs Postgres on localhost:5432 with database ihelper_e2e (docker compose up -d postgres).
      // The e2e profile wipes and re-seeds that database on every start.
      command: "node start-backend.mjs",
      url: `http://localhost:${BACKEND_PORT}/api/v1/categories`,
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
      stdout: "pipe",
      stderr: "pipe",
    },
    {
      command: `npm run dev -- --port ${FRONTEND_PORT} --strictPort`,
      cwd: "../frontend",
      env: { API_PROXY_TARGET: `http://localhost:${BACKEND_PORT}` },
      url: `http://localhost:${FRONTEND_PORT}`,
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
    },
  ],
});
