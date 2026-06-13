// L1 session memory — SDK manages the context window directly.
// This module is a thin passthrough for Demo.

interface Message {
  role: "user" | "assistant";
  content: string;
}

const messages = new Map<string, Message[]>();

export function getRecent(sessionId: string, limit: number): Message[] {
  const msgs = messages.get(sessionId) ?? [];
  return msgs.slice(-limit);
}

export function save(
  sessionId: string,
  userMsg: string,
  assistantMsg: string,
): void {
  if (!messages.has(sessionId)) {
    messages.set(sessionId, []);
  }
  const msgs = messages.get(sessionId)!;
  msgs.push({ role: "user", content: userMsg });
  msgs.push({ role: "assistant", content: assistantMsg });
}
