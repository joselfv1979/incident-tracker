import { randomUUID } from "node:crypto";
import { promoteToAgent, register, verifyEmail } from "./api";
import { PASSWORD } from "./env";
import { waitForVerificationToken } from "./mailpit";

export interface TestUser {
  name: string;
  email: string;
  password: string;
}

/** A unique user, so tests can run in parallel without clashing. */
export function newUser(prefix: string): TestUser {
  const id = randomUUID().slice(0, 8);
  return {
    name: `E2E ${prefix} ${id}`,
    email: `e2e.${prefix}.${id}@example.com`,
    password: PASSWORD,
  };
}

export async function createVerifiedUser(prefix: string): Promise<TestUser> {
  const user = newUser(prefix);
  await register(user.name, user.email, user.password);
  await verifyEmail(await waitForVerificationToken(user.email));
  return user;
}

export async function createAgent(prefix: string): Promise<TestUser> {
  const user = await createVerifiedUser(prefix);
  await promoteToAgent(user.email);
  return user;
}
