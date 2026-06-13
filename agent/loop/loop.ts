import Anthropic from "@anthropic-ai/sdk";
import type { LoopInput, LoopConfig } from "./types";
import type { Tool, ToolContext } from "../tools/types";
import { renderTemplate } from "../prompts/loader";

const DEFAULT_CONFIG: LoopConfig = {
  apiKey: process.env.BUBBLE_API_KEY || process.env.DEEPSEEK_API_KEY || "",
  baseUrl: process.env.BUBBLE_BASE_URL || "https://api.deepseek.com/anthropic",
  model: process.env.BUBBLE_MODEL || "deepseek-chat",
  maxTokens: parseInt(process.env.BUBBLE_MAX_TOKENS || "512", 10),
};

const TOOL_CALL_RE = /```tool_call\s*\n([\s\S]*?)\n```/g;

export async function* runLoop(
  input: LoopInput,
  config: Partial<LoopConfig> = {},
  toolCtx: ToolContext,
): AsyncIterable<string> {
  const cfg = { ...DEFAULT_CONFIG, ...config };
  const client = new Anthropic({ apiKey: cfg.apiKey, baseURL: cfg.baseUrl });

  const toolMap = new Map(input.tools.map((t) => [t.definition.name, t]));

  let messages = input.messages.map((m) => ({
    role: m.role as "user" | "assistant",
    content: m.content,
  }));

  let systemPrompt = input.systemPrompt;

  if (input.tools.length > 0) {
    systemPrompt = injectToolsIntoPrompt(systemPrompt, input.tools);
  }

  let iteration = 0;

  while (iteration < 5) {
    iteration++;

    const stream = client.messages.stream({
      model: cfg.model,
      max_tokens: cfg.maxTokens,
      system: systemPrompt,
      messages,
    });

    let fullText = "";

    for await (const event of stream) {
      if (
        event.type === "content_block_delta" &&
        event.delta.type === "text_delta"
      ) {
        fullText += event.delta.text;
      }
    }

    // check for tool calls in text (prompt mode)
    const toolCalls = parseToolCalls(fullText);

    if (toolCalls.length > 0) {
      // add assistant message with full text
      messages.push({ role: "assistant", content: fullText });

      // execute tools
      const toolResults: string[] = [];
      for (const tc of toolCalls) {
        const tool = toolMap.get(tc.name);
        if (tool) {
          try {
            const result = await tool.handler(tc.params, toolCtx);
            toolResults.push(
              `工具 ${tc.name} 执行成功: ${JSON.stringify(result)}`,
            );
          } catch (err) {
            toolResults.push(
              `工具 ${tc.name} 执行失败: ${err instanceof Error ? err.message : String(err)}`,
            );
          }
        }
      }

      messages.push({
        role: "user",
        content: toolResults.join("\n") + "\n\n请根据工具结果回复用户。",
      });

      // continue loop to let LLM process tool results
    } else {
      // no tool calls, yield text and done
      yield fullText;
      break;
    }
  }
}

function injectToolsIntoPrompt(systemPrompt: string, tools: Tool[]): string {
  const toolDescs = tools
    .map((t) => {
      const params = Object.entries(t.definition.parameters.properties)
        .map(([k, v]) => `  ${k}: ${(v as { description?: string }).description ?? ""}`)
        .join("\n");
      return `- ${t.definition.name}: ${t.definition.description}\n参数:\n${params}`;
    })
    .join("\n\n");

  const guide = renderTemplate("toolGuide.md", { toolDescs });

  return systemPrompt + "\n\n" + guide;
}

function parseToolCalls(text: string): Array<{ name: string; params: Record<string, unknown> }> {
  const calls: Array<{ name: string; params: Record<string, unknown> }> = [];
  const matches = text.matchAll(TOOL_CALL_RE);

  for (const m of matches) {
    try {
      const parsed = JSON.parse(m[1].trim());
      if (parsed.name && parsed.params) {
        calls.push(parsed);
      }
    } catch {
      // ignore malformed tool calls
    }
  }

  return calls;
}
