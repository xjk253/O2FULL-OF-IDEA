import { describe, it, expect, beforeEach } from "vitest";
import { preprocess } from "./preprocess";
import { PetStateMachine } from "../petstate/machine";
import { Mood } from "../petstate/types";
import { buildPrompt } from "../prompts/factory";
import type { CharacterConfig } from "../character/schema";
import type { EmoMap } from "../emoMap/types";

const character: CharacterConfig = {
  name: "泡泡",
  confUid: "bubble-v1",
  live2dModelName: "penguin-bubble",
  personaPrompt: "你是泡泡，一只圆滚滚的小企鹅。",
};

const emoMap: EmoMap = {
  name: "test",
  emotionMap: { happy: 0, excited: 1, sad: 2 },
};

function makeDeps() {
  return {
    stateMachine: new PetStateMachine(),
    trigger: {
      check: () => null, // never triggers
    },
    memory: {
      getRecent: () => [],
    },
    promptFactory: { build: buildPrompt },
    character,
    emoMap,
    tools: [],
  };
}

describe("preprocess", () => {
  it("new session → returns 'continue' with prompt", () => {
    const deps = makeDeps();
    const result = preprocess(
      { text: "泡泡～", sessionId: "s1" },
      deps,
    );

    expect(result.kind).toBe("continue");
    expect(result.systemPrompt).toContain("你是泡泡");
  });

  it("user message includes history if available", () => {
    const deps = makeDeps();
    deps.memory.getRecent = () => [
      { role: "user" as const, content: "你好" },
      { role: "assistant" as const, content: "诶嘿～" },
    ];

    const result = preprocess(
      { text: "我又来了", sessionId: "s1" },
      deps,
    );

    expect(result.kind).toBe("continue");
    expect(result.messages).toHaveLength(3); // 2 history + 1 new
  });

  it("shortCircuit when trigger fires", () => {
    const deps = makeDeps();
    deps.trigger.check = () => ({
      toSentenceOutput: () => ({
        displayText: "[sad] 主人……你是不是把我忘了……",
        ttsText: "主人……你是不是把我忘了……",
        actions: { expressions: [2] },
      }),
    });

    const result = preprocess(
      { text: "泡泡～", sessionId: "s1" },
      deps,
    );

    expect(result.kind).toBe("shortCircuit");
    if (result.kind === "shortCircuit") {
      expect(result.output.ttsText).toContain("你是不是把我忘了");
    }
  });

  it("transition is called on userActive", () => {
    const deps = makeDeps();
    deps.stateMachine.getState("s1");

    preprocess({ text: "泡泡～", sessionId: "s1" }, deps);

    const state = deps.stateMachine.getState("s1");
    expect(state.mood).toBe(Mood.Happy);
  });
});
