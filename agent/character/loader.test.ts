import { describe, it, expect } from "vitest";
import { loadCharacter } from "./loader";
import { resolve } from "node:path";

const fixture = resolve(import.meta.dirname ?? ".", "config.yaml");

describe("character/loader", () => {
  it("loads 泡泡's character config", () => {
    const cfg = loadCharacter(fixture);
    expect(cfg.name).toBe("泡泡");
    expect(cfg.confUid).toBe("bubble-v1");
    expect(cfg.personaPrompt).toContain("圆滚滚的小企鹅");
    expect(cfg.personaPrompt).toContain("诶嘿～");
  });

  it("throws on missing file", () => {
    expect(() => loadCharacter("/nonexistent.yaml")).toThrow();
  });
});
