import type { CharacterConfig } from "../character/schema";
import type { PetState } from "../petstate/types";
import type { EmoMap } from "../emoMap/types";
import { Mood } from "../petstate/types";
import { loadTemplate, renderTemplate } from "./loader";

// tone templates loaded from .md files
const toneFile: Record<Mood, string> = {
  [Mood.Normal]: "tones/normal.md",
  [Mood.Happy]: "tones/happy.md",
  [Mood.Inspired]: "tones/inspired.md",
  [Mood.Lonely]: "tones/lonely.md",
};

const toneCache = new Map<Mood, string>();

function getTone(mood: Mood): string {
  if (!toneCache.has(mood)) {
    toneCache.set(mood, loadTemplate(toneFile[mood]).trim());
  }
  return toneCache.get(mood)!;
}

function buildExpressionGuide(emoMap: EmoMap): string {
  const keywords = Object.keys(emoMap.emotionMap)
    .map((k) => `[${k}]`)
    .join(" ");
  return renderTemplate("expression.md", { emoKeywords: keywords });
}

function buildThinkTagGuide(): string {
  return loadTemplate("thinkTag.md");
}

function buildDateInfo(): string {
  const now = new Date();
  const weekDay = ["日", "一", "二", "三", "四", "五", "六"][now.getDay()];
  const h = now.getHours();
  const m = String(now.getMinutes()).padStart(2, "0");
  return `现在是 ${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日，${h}:${m}。周${weekDay}。主人问时间/日期时，直接告诉主人，不要说不知道。`;
}

export function buildPrompt(
  character: CharacterConfig,
  petState: PetState,
  emoMap: EmoMap,
  warmMemoryContext = "",
): string {
  const parts = [
    character.personaPrompt,
    buildExpressionGuide(emoMap),
    buildThinkTagGuide(),
    getTone(petState.mood),
    buildDateInfo(),
  ];

  if (warmMemoryContext) {
    parts.splice(1, 0, warmMemoryContext);
  }

  return parts.join("\n\n");
}
