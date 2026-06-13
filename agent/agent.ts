import type { SentenceOutput } from "./postprocess/types";
import type { LoopConfig } from "./loop/types";
import type { ToolContext } from "./tools/types";

export interface AgentConfig {
  character: import("./character/schema").CharacterConfig;
  emoMap: import("./emoMap/types").EmoMap;
  loopConfig?: Partial<LoopConfig>;
}

export interface Agent {
  chat(text: string, sessionId: string): AsyncIterable<SentenceOutput>;
}

export function createAgent(config: AgentConfig): Agent {
  return {
    async *chat(text: string, sessionId: string) {
      // placeholder — full implementation wires preprocess → loop → postprocess
      // gateway/server.ts calls the modules directly for now
      yield {
        displayText: text,
        ttsText: text,
        actions: { expressions: [] },
      };
    },
  };
}
