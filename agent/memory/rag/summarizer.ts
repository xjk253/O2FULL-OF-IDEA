import Anthropic from "@anthropic-ai/sdk";
import type { RagEntry } from "./types";

interface Message {
  role: "user" | "assistant";
  content: string;
}

const SUMMARY_SYSTEM_PROMPT = `你是一个对话总结助手。请总结以下对话的关键信息，包括：
- 讨论的主要话题
- 用户提到的重要事实（喜好、经历、计划等）
- 达成的共识或决定

要求：
1. 总结用简洁的中文，不超过 200 字
2. 提取 5-10 个关键词，用于后续检索
3. 只输出 JSON，格式：{"summary": "...", "keywords": ["关键词1", "关键词2", ...]}
4. 不要输出任何其他内容`;

export async function summarize(
  sessionId: string,
  messages: Message[],
): Promise<RagEntry | null> {
  if (messages.length < 2) {
    return null;
  }

  const apiKey = process.env.DEEPSEEK_API_KEY || "";
  if (!apiKey) {
    console.error("[rag/summarizer] DEEPSEEK_API_KEY 未设置，跳过总结");
    return null;
  }

  const client = new Anthropic({
    apiKey,
    baseURL: "https://api.deepseek.com/anthropic",
  });

  const conversationText = messages
    .map((m) => `${m.role === "user" ? "用户" : "泡泡"}: ${m.content}`)
    .join("\n");

  const startTime = new Date(Date.now() - 3600_000).toISOString();
  const endTime = new Date().toISOString();

  try {
    const response = await client.messages.create({
      model: "deepseek-chat",
      max_tokens: 512,
      system: SUMMARY_SYSTEM_PROMPT,
      messages: [{ role: "user", content: conversationText }],
    });

    const text = response.content
      .filter((c): c is Anthropic.TextBlock => c.type === "text")
      .map((c) => c.text)
      .join("");

    const parsed = extractJson(text);
    if (!parsed) {
      console.error("[rag/summarizer] 无法从 LLM 响应解析 JSON: " + text.slice(0, 100));
      return null;
    }

    return {
      sessionId,
      startTime,
      endTime,
      summary: String(parsed.summary || ""),
      keywords: Array.isArray(parsed.keywords) ? parsed.keywords.map(String) : [],
      messageCount: messages.length,
    };
  } catch (err) {
    console.error("[rag/summarizer] LLM 调用失败: " + String(err));
    return null;
  }
}

function extractJson(text: string): { summary?: string; keywords?: string[] } | null {
  // 先尝试直接 parse
  try {
    return JSON.parse(text);
  } catch {
    // fall through
  }
  // 再尝试从文本中提取 {...} 子串
  const match = text.match(/\{[\s\S]*\}/);
  if (match) {
    try {
      return JSON.parse(match[0]);
    } catch {
      // fall through
    }
  }
  return null;
}
