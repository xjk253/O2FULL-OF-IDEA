import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { EmoMap } from "./types";

export function loadEmoMap(filePath: string): EmoMap {
  const absPath = resolve(filePath);
  const raw = readFileSync(absPath, "utf-8");
  const data: EmoMap = JSON.parse(raw);

  if (!data.name || !data.emotionMap) {
    throw new Error(`Invalid emoMap: ${absPath}`);
  }

  for (const [keyword, id] of Object.entries(data.emotionMap)) {
    if (typeof id !== "number") {
      throw new Error(`Invalid expression id for "${keyword}": ${id}`);
    }
  }

  return data;
}
