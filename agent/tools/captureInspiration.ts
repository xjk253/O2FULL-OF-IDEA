import type { ToolDefinition, ToolHandler, ToolResult } from "./types";

export const definition: ToolDefinition = {
  name: "captureInspiration",
  description:
    "当用户在聊天中提到一个可以执行的灵感、想法、计划时调用此工具。只在用户确实表达了要做某事时才调用，闲聊或倾诉时不要调用。",
  parameters: {
    type: "object",
    properties: {
      title: { type: "string", description: "灵感的一句话标题" },
      description: { type: "string", description: "用户说了什么" },
      category: {
        type: "string",
        enum: ["work", "life", "creative", "study", "other"],
        description: "灵感分类",
      },
    },
    required: ["title", "description"],
  },
};

export const handler: ToolHandler = async (params) => {
  const { title, description, category } = params as {
    title: string;
    description: string;
    category?: string;
  };

  const result: ToolResult = {
    type: "inspirationCard",
    title,
    description,
    category: category ?? "other",
    capturedAt: Date.now(),
  };

  return result;
};
