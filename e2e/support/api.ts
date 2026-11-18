import { ADMIN_EMAIL, ADMIN_PASSWORD, API_URL } from "./env";

async function call<T>(
  path: string,
  method: string,
  body?: unknown,
  token?: string,
): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`${method} ${path} failed with ${response.status}`);
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export async function login(email: string, password: string): Promise<string> {
  const { accessToken } = await call<{ accessToken: string }>(
    "/auth/login",
    "POST",
    { email, password },
  );
  return accessToken;
}

export const register = (name: string, email: string, password: string) =>
  call("/auth/register", "POST", { name, email, password });

export const verifyEmail = (token: string) =>
  call("/auth/verify-email", "POST", { token });

export async function createIncident(
  token: string,
  title: string,
): Promise<number> {
  const incident = await call<{ id: number }>(
    "/incidents",
    "POST",
    { title, description: "Created by an end-to-end test" },
    token,
  );
  return incident.id;
}

export async function promoteToAgent(email: string): Promise<void> {
  const adminToken = await login(ADMIN_EMAIL, ADMIN_PASSWORD);
  // The newest users come first, so the one just created is on the first page
  const page = await call<{ content: { id: number; email: string }[] }>(
    "/users?page=0&size=100",
    "GET",
    undefined,
    adminToken,
  );
  const user = page.content.find((candidate) => candidate.email === email);
  if (!user) throw new Error(`User ${email} not found`);
  await call(`/users/${user.id}/role`, "PATCH", { role: "AGENT" }, adminToken);
}
