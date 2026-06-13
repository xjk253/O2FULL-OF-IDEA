import type { Tool, ToolContext } from "./types";
import * as captureInspiration from "./captureInspiration";
import * as webSearch from "./webSearch";
import * as bash from "./bash";

export function createTools(_ctx: ToolContext): Tool[] {
  return [
    {
      definition: captureInspiration.definition,
      handler: captureInspiration.handler,
    },
    {
      definition: webSearch.definition,
      handler: webSearch.handler,
    },
    {
      definition: bash.definition,
      handler: bash.handler,
    },
  ];
}

export type { Tool, ToolDefinition, ToolHandler, ToolContext, ToolResult } from "./types";
