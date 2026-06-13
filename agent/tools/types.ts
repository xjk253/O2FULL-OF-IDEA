export interface ToolDefinition {
  name: string;
  description: string;
  parameters: {
    type: "object";
    properties: Record<string, unknown>;
    required: string[];
  };
}

export type ToolHandler = (
  params: Record<string, unknown>,
  ctx: ToolContext,
) => Promise<ToolResult>;

export interface Tool {
  definition: ToolDefinition;
  handler: ToolHandler;
}

export interface ToolContext {
  character: { name: string };
  petState: { mood: string; affinity: number };
  sessionId: string;
}

export interface ToolResult {
  type: string;
  title: string;
  description: string;
  category: string;
  capturedAt: number;
}
