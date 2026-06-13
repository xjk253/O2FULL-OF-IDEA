import { describe, it, expect } from "vitest";
import { composePipeline } from "./compose";
import type { EmoMap } from "../emoMap/types";

const emoMap: EmoMap = {
  name: "test",
  emotionMap: { happy: 0, excited: 1, sad: 2 },
};

describe("postprocess pipeline", () => {
  it("extracts single expression from one sentence", async () => {
    const pipeline = composePipeline(emoMap);
    const tokens = streamFrom("[happy] 主人你来啦！");
    const results = await collect(pipeline(tokens));

    expect(results[0].displayText).toBe("[happy] 主人你来啦！");
    expect(results[0].ttsText).toBe("主人你来啦！");
    expect(results[0].actions.expressions).toEqual([0]);
  });

  it("extracts multiple expressions across sentences", async () => {
    const pipeline = composePipeline(emoMap);
    const tokens = streamFrom("[happy] 主人！[excited] 我帮你做完了！");
    const results = await collect(pipeline(tokens));

    expect(results).toHaveLength(2);
    expect(results[0].actions.expressions).toEqual([0]); // happy
    expect(results[1].actions.expressions).toEqual([1]); // excited
  });

  it("converts <think/> to parentheses in display, strips from tts", async () => {
    const pipeline = composePipeline(emoMap);
    const tokens = streamFrom(
      "<think/>主人不开心</think/>[sad] 主人……你还好吗？",
    );
    const results = await collect(pipeline(tokens));

    expect(results[0].displayText).toContain("(主人不开心)");
    expect(results[0].ttsText).toBe("主人……你还好吗？");
  });

  it("no expression keyword → empty expressions array", async () => {
    const pipeline = composePipeline(emoMap);
    const tokens = streamFrom("主人你好呀。");
    const results = await collect(pipeline(tokens));

    expect(results[0].actions.expressions).toEqual([]);
  });

  it("unknown expression keyword → ignored", async () => {
    const pipeline = composePipeline(emoMap);
    const tokens = streamFrom("[unknown] 主人你好。");
    const results = await collect(pipeline(tokens));

    expect(results[0].actions.expressions).toEqual([]);
  });

  it("handles multi-sentence <think/> tag", async () => {
    const pipeline = composePipeline(emoMap);
    const tokens = streamFrom(
      "<think/>主人不开心。他好像很累。</think/>[sad] 主人……你还好吗？",
    );
    const results = await collect(pipeline(tokens));

    expect(results[0].displayText).toContain("(主人不开心。他好像很累。)");
    expect(results[0].ttsText).toBe("主人……你还好吗？");
  });

  it("strips multiple expression tags from tts", async () => {
    const pipeline = composePipeline(emoMap);
    const tokens = streamFrom("[happy][excited] 主人主人！");
    const results = await collect(pipeline(tokens));

    expect(results[0].ttsText).toBe("主人主人！");
  });
});

// helpers

async function* streamFrom(text: string) {
  // simulate token-by-token streaming
  yield text;
}

async function collect<T>(stream: AsyncIterable<T>): Promise<T[]> {
  const items: T[] = [];
  for await (const item of stream) {
    items.push(item);
  }
  return items;
}
