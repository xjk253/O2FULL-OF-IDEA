import type { Tool } from "../tools/types";

export interface LoopInput {
  systemPrompt: string;
  messages: Array<{ role: "user" | "assistant"; content: string }>;
  tools: Tool[];
}

export interface LoopConfig {
  apiKey: string;
  baseUrl: string;
  model: string;
  maxTokens: number;
}
