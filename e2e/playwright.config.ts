import { defineConfig, devices } from "@playwright/test";
import { ADMIN_EMAIL, ADMIN_PASSWORD, FRONTEND_URL } from "./support/env";

export default defineConfig({
  testDir: "./tests",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 2 : undefined,
  reporter: process.env.CI
    ? [["github"], ["html", { open: "never" }]]
    : [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: FRONTEND_URL,
    // Locally there are no retries, so keep the trace of every failure
    trace: process.env.CI ? "on-first-retry" : "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      // The environment variables below override whatever the terminal has, so the
      // E2E backend never talks to the development database
      command: "./mvnw -q spring-boot:run",
      cwd: "../backend",
      url: "http://localhost:8081/api/health",
      reuseExistingServer: false,
      timeout: 180_000,
      env: {
        SERVER_PORT: "8081",
        DB_URL: "jdbc:postgresql://localhost:5433/e2e",
        DB_USER: "e2e",
        DB_PASSWORD: "e2e",
        JWT_SECRET: "e2e-only-secret-0123456789abcdef0123456789abcdef",
        ADMIN_EMAIL,
        ADMIN_PASSWORD,
        MAIL_HOST: "localhost",
        MAIL_PORT: "1026",
        APP_BASE_URL: FRONTEND_URL,
        API_DOCS_ENABLED: "false",
        RATE_LIMIT_ENABLED: "false",
      },
    },
    {
      command: "pnpm dev --port 5174 --strictPort",
      cwd: "../frontend",
      url: FRONTEND_URL,
      reuseExistingServer: false,
      timeout: 60_000,
      env: { API_PROXY_TARGET: "http://localhost:8081" },
    },
  ],
});
