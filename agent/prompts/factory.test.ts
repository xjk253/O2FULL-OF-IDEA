import { describe, it, expect } from "vitest";
import { buildPrompt } from "./factory";
import { Mood } from "../petstate/types";
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
  emotionMap: { happy: 0, sad: 1 },
};

function state(mood: Mood) {
  return { mood, affinity: 50, lastActiveTime: Date.now() };
}

describe("prompts/factory", () => {
  it("includes persona in output", () => {
    const prompt = buildPrompt(character, state(Mood.Normal), emoMap);
    expect(prompt).toContain("你是泡泡");
  });

  it("includes emoMap keywords", () => {
    const prompt = buildPrompt(character, state(Mood.Normal), emoMap);
    expect(prompt).toContain("[happy]");
    expect(prompt).toContain("[sad]");
  });

  it("includes think tag guide", () => {
    const prompt = buildPrompt(character, state(Mood.Normal), emoMap);
    expect(prompt).toContain("<think/>");
  });

  it("happy mood → different tone than normal", () => {
    const normal = buildPrompt(character, state(Mood.Normal), emoMap);
    const happy = buildPrompt(character, state(Mood.Happy), emoMap);
    expect(happy).not.toBe(normal);
    expect(happy).toContain("主人来找泡泡了");
  });

  it("lonely mood → quiet tone", () => {
    const prompt = buildPrompt(character, state(Mood.Lonely), emoMap);
    expect(prompt).toContain("主人好久没来了");
  });

  it("includes current date and time", () => {
    const prompt = buildPrompt(character, state(Mood.Normal), emoMap);
    expect(prompt).toContain("现在是");
    expect(prompt).toContain("年");
    expect(prompt).toContain("月");
    expect(prompt).toContain("日");
  });
});
