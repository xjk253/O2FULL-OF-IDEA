import type { PetStateMachine } from "../petstate/machine";
import type { CharacterConfig } from "../character/schema";
import type { EmoMap } from "../emoMap/types";
import type { PreProcessInput, PreProcessResult } from "./types";

interface Trigger {
  check(state: { mood: string; affinity: number; lastActiveTime: number }, now: number): {
    toSentenceOutput(): { displayText: string; ttsText: string; actions: { expressions: number[] } };
  } | null;
}

interface Memory {
  getRecent(sessionId: string, limit: number): Array<{ role: "user" | "assistant"; content: string }>;
}

interface PromptFactory {
  build(character: CharacterConfig, petState: { mood: string; affinity: number; lastActiveTime: number }, emoMap: EmoMap, warmMemoryContext?: string): string;
}

interface PreProcessDeps {
  stateMachine: PetStateMachine;
  trigger: Trigger;
  memory: Memory;
  promptFactory: PromptFactory;
  character: CharacterConfig;
  emoMap: EmoMap;
  tools: unknown[];
  warmMemory: { getContext(): string } | null;
  ragMemory: { retrieve(query: string): string } | null;
}

export function preprocess(
  input: PreProcessInput,
  deps: PreProcessDeps,
): PreProcessResult {
  // Step 1: read state
  const state = deps.stateMachine.getState(input.sessionId);

  // Step 2: update state
  deps.stateMachine.transition(input.sessionId, { type: "userActive" }, Date.now());

  // Step 3: check triggers
  const trigger = deps.trigger.check(state, Date.now());
  if (trigger) {
    const output = trigger.toSentenceOutput();
    return { kind: "shortCircuit", output };
  }

  // Step 4: retrieve memory
  const history = deps.memory.getRecent(input.sessionId, 10);

  // Step 5: build system prompt (with warm memory + RAG memory if available)
  const warmContext = deps.warmMemory?.getContext() ?? "";
  const ragContext = deps.ragMemory ? deps.ragMemory.retrieve(input.text) : "";
  const extraContext = [warmContext, ragContext].filter((s) => s.length > 0).join("\n\n");
  const systemPrompt = deps.promptFactory.build(deps.character, state, deps.emoMap, extraContext);

  // Step 6: assemble
  return {
    kind: "continue",
    systemPrompt,
    messages: [...history, { role: "user", content: input.text }],
  };
}
