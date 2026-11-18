import { MAILPIT_URL } from "./env";

interface SearchResult {
  messages?: { ID: string }[];
}

interface MessageDetail {
  Text: string;
}

/** Waits for the verification email of an address and returns the token in its link. */
export async function waitForVerificationToken(
  email: string,
  timeoutMs = 20_000,
): Promise<string> {
  const deadline = Date.now() + timeoutMs;

  while (Date.now() < deadline) {
    const search = await fetch(
      `${MAILPIT_URL}/api/v1/search?query=${encodeURIComponent(`to:${email}`)}`,
    );
    const { messages = [] } = (await search.json()) as SearchResult;

    // The newest message comes first
    if (messages.length > 0) {
      const detail = await fetch(
        `${MAILPIT_URL}/api/v1/message/${messages[0]!.ID}`,
      );
      const { Text } = (await detail.json()) as MessageDetail;
      const match = /token=([A-Za-z0-9_-]+)/.exec(Text);
      if (match) return match[1]!;
    }
    await new Promise((resolve) => setTimeout(resolve, 300));
  }

  throw new Error(`No verification email arrived for ${email}`);
}
