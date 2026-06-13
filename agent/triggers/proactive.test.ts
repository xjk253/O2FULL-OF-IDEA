import { describe, it, expect } from "vitest";
import { checkProactive } from "./proactive";
import { Mood } from "../petstate/types";

function state(mood: Mood, lastActiveAgeMs: number) {
  return {
    mood,
    affinity: 50,
    lastActiveTime: Date.now() - lastActiveAgeMs,
  };
}

describe("triggers/proactive", () => {
  it("idle > 4h → lonely message", () => {
    const result = checkProactive(state(Mood.Normal, 5 * 3600_000));
    expect(result).not.toBeNull();
    expect(result!.type).toBe("proactiveSpeak");
    expect(result!.message).toContain("主人");
    expect(result!.expression).toBe("sad");
  });

  it("idle < 4h → no trigger", () => {
    const result = checkProactive(state(Mood.Normal, 1 * 3600_000));
    expect(result).toBeNull();
  });

  it("already lonely → no duplicate trigger", () => {
    const result = checkProactive(state(Mood.Lonely, 5 * 3600_000));
    expect(result).toBeNull();
  });

  it("active user → no trigger", () => {
    const result = checkProactive(state(Mood.Happy, 60_000));
    expect(result).toBeNull();
  });
});
