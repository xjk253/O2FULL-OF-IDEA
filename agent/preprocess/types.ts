import type { SentenceOutput } from "../postprocess/types";

export interface PreProcessInput {
  text: string;
  sessionId: string;
}

export type PreProcessResult = ShortCircuit | ContinueToLoop;

export interface ShortCircuit {
  kind: "shortCircuit";
  output: SentenceOutput;
}

export interface ContinueToLoop {
  kind: "continue";
  systemPrompt: string;
  messages: Array<{ role: "user" | "assistant"; content: string }>;
}
