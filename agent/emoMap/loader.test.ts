import { describe, it, expect } from "vitest";
import { loadEmoMap } from "./loader";
import { resolve } from "node:path";

const fixture = resolve(import.meta.dirname ?? ".", "modelDict.json");

describe("emoMap/loader", () => {
  it("loads and validates a valid emoMap", () => {
    const emoMap = loadEmoMap(fixture);
    expect(emoMap.name).toBe("penguin-bubble");
    expect(emoMap.emotionMap["happy"]).toBe(0);
    expect(emoMap.emotionMap["thinking"]).toBe(7);
  });

  it("throws on missing file", () => {
    expect(() => loadEmoMap("/nonexistent.json")).toThrow();
  });
});
