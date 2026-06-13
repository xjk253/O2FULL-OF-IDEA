import type { EmoMap } from "../emoMap/types";
import { sentenceDivider } from "./sentenceDivider";
import { extractActions } from "./actionsExtractor";
import { processDisplay } from "./displayProcessor";
import { filterTts } from "./ttsFilter";
import type { SentenceOutput } from "./types";

export type Pipeline = (
  tokens: AsyncIterable<string>,
) => AsyncIterable<SentenceOutput>;

export function composePipeline(emoMap: EmoMap): Pipeline {
  return async function* (tokens) {
    for await (const sentence of sentenceDivider(tokens)) {
      const actions = extractActions(sentence, emoMap);
      const displayText = processDisplay(sentence);
      const ttsText = filterTts(sentence);

      yield { displayText, ttsText, actions };
    }
  };
}
