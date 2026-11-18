export const FRONTEND_URL =
  process.env.E2E_FRONTEND_URL ?? "http://localhost:5174";
export const API_URL = process.env.E2E_API_URL ?? "http://localhost:8081/api";
export const MAILPIT_URL =
  process.env.E2E_MAILPIT_URL ?? "http://localhost:8026";

// Throwaway credentials of the isolated E2E environment
export const ADMIN_EMAIL = "e2e-admin@example.com";
export const ADMIN_PASSWORD = "e2e-admin-password";
export const PASSWORD = "e2e-password-123";
