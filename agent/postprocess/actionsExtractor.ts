import type { EmoMap } from "../emoMap/types";
import type { Actions } from "./types";

export function extractActions(text: string, emoMap: EmoMap): Actions {
  const expressions: number[] = [];
  const pattern = /\[([a-zA-Z]+)\]/g;
  let match: RegExpExecArray | null;

  while ((match = pattern.exec(text)) !== null) {
    const keyword = match[1].toLowerCase();
    const id = emoMap.emotionMap[keyword];
    if (id !== undefined) {
      expressions.push(id);
    }
  }

  return { expressions };
}
