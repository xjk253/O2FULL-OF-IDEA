import type { ToolDefinition, ToolHandler, ToolResult } from "./types";

export const definition: ToolDefinition = {
  name: "webSearch",
  description:
    "搜索互联网获取实时信息。当主人问的问题你无法确定答案、需要最新信息、或者今天发生了什么时使用。",
  parameters: {
    type: "object",
    properties: {
      query: { type: "string", description: "搜索关键词" },
    },
    required: ["query"],
  },
};

export const handler: ToolHandler = async (params) => {
  const { query } = params as { query: string };
  const q = encodeURIComponent(query);

  try {
    const resp = await fetch(
      `https://html.duckduckgo.com/html/?q=${q}`,
      {
        headers: { "User-Agent": "Bubble/1.0" },
        signal: AbortSignal.timeout(10000),
      },
    );

    const html = await resp.text();

    // extract snippets from DDG results
    const snippets: string[] = [];
    const matches = html.matchAll(
      /class="result__snippet"[^>]*>([^<]+)</g,
    );
    for (const m of matches) {
      const text = m[1]
        .replace(/&amp;/g, "&")
        .replace(/&lt;/g, "<")
        .replace(/&gt;/g, ">")
        .replace(/&quot;/g, '"')
        .replace(/&#x27;/g, "'");
      snippets.push(text.trim());
    }

    if (snippets.length === 0) {
      return {
        type: "searchResult",
        title: query,
        description: "没有找到相关结果",
        category: "web",
        capturedAt: Date.now(),
      } as ToolResult;
    }

    return {
      type: "searchResult",
      title: query,
      description: snippets.slice(0, 5).join("\n\n"),
      category: "web",
      capturedAt: Date.now(),
    } as ToolResult;
  } catch (err) {
    return {
      type: "searchResult",
      title: query,
      description: `搜索失败: ${err instanceof Error ? err.message : "未知错误"}`,
      category: "web",
      capturedAt: Date.now(),
    } as ToolResult;
  }
};
