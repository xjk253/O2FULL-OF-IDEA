import { list } from "./store";
import type { RagEntry } from "./types";

/**
 * 检索与查询相关的历史记忆。
 *
 * 算法：反向匹配。
 * 中文没有空格分词，无法可靠地对 query 分词，
 * 因此反向遍历每条 RagEntry 的 keywords + summary 中的关键短语，
 * 检查它们是否作为子串出现在 query 里。
 *
 * 例如 keyword "数字" 会匹配到 query "我刚刚说的数字是什么"。
 *
 * 打分规则：
 * - keyword 作为子串出现在 query 里：+3 分（强信号）
 * - summary 中的 2-4 字片段出现在 query 里：+1 分（弱信号）
 *
 * 按总分降序，取 topK。
 */
export function retrieve(query: string, topK = 3): string {
  const trimmed = query.trim().toLowerCase();
  if (trimmed.length === 0) return "";

  const entries = list();
  if (entries.length === 0) return "";

  const scored = entries.map((entry) => ({ entry, score: scoreEntry(entry, trimmed) }));

  const relevant = scored
    .filter((s) => s.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, topK);

  if (relevant.length === 0) return "";

  const lines = relevant.map((s) => {
    const date = s.entry.endTime.slice(0, 10);
    return `- [${date}] ${s.entry.summary}`;
  });

  return "## 相关历史记忆\n" + lines.join("\n");
}

function scoreEntry(entry: RagEntry, query: string): number {
  let score = 0;

  // 1. keyword 作为子串出现在 query 里（强信号）
  for (const keyword of entry.keywords) {
    const kw = keyword.toLowerCase();
    if (kw.length >= 2 && query.includes(kw)) {
      score += 3;
    }
  }

  // 2. summary 中的关键片段出现在 query 里（弱信号）
  //    提取 summary 中的 2-4 字连续中文/数字片段
  const phrases = extractPhrases(entry.summary);
  for (const phrase of phrases) {
    if (phrase.length >= 2 && query.includes(phrase.toLowerCase())) {
      score += 1;
    }
  }

  return score;
}

/**
 * 从 summary 中提取候选关键短语。
 * 简单策略：按标点/空格切分后，保留长度 2-8 的片段。
 */
function extractPhrases(summary: string): string[] {
  return summary
    .split(/[，。！？,.!?；;：:\s、（）()「」""''\-—…]+/)
    .map((s) => s.trim())
    .filter((s) => s.length >= 2 && s.length <= 8);
}
