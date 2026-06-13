import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { load as loadYaml } from "js-yaml";
import type { CharacterConfig } from "./schema";

export function loadCharacter(filePath: string): CharacterConfig {
  const absPath = resolve(filePath);
  const raw = readFileSync(absPath, "utf-8");
  const data = loadYaml(raw) as Record<string, unknown>;

  if (!data.name || !data.personaPrompt) {
    throw new Error(`Invalid character config: ${absPath}`);
  }

  return {
    name: String(data.name),
    confUid: String(data.confUid ?? ""),
    live2dModelName: String(data.live2dModelName ?? ""),
    personaPrompt: String(data.personaPrompt),
  };
}
