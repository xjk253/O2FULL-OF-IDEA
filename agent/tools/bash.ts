import { exec } from "node:child_process";
import { promisify } from "node:util";
import type { ToolDefinition, ToolHandler, ToolResult } from "./types";

const execAsync = promisify(exec);

export const definition: ToolDefinition = {
  name: "runBash",
  description:
    "在电脑上执行一个 bash 命令并返回结果。当主人需要查询文件、运行程序、获取系统信息时使用。只用于安全无害的命令。",
  parameters: {
    type: "object",
    properties: {
      command: {
        type: "string",
        description: "要执行的 bash 命令。避免 destructive 操作。",
      },
    },
    required: ["command"],
  },
};

const BLOCKED = ["rm ", "format ", "mkfs", "dd ", "> /dev"];

export const handler: ToolHandler = async (params) => {
  const { command } = params as { command: string };

  // safety check
  const lower = command.toLowerCase();
  for (const blocked of BLOCKED) {
    if (lower.includes(blocked)) {
      return {
        type: "bashResult",
        title: command,
        description: `命令被拦截: 包含危险操作 "${blocked}"`,
        category: "system",
        capturedAt: Date.now(),
      } as ToolResult;
    }
  }

  try {
    const { stdout, stderr } = await execAsync(command, {
      timeout: 15000,
      maxBuffer: 1024 * 500,
      cwd: process.env.HOME || "/tmp",
    });

    const output = [stdout, stderr].filter(Boolean).join("\n") || "(无输出)";

    return {
      type: "bashResult",
      title: command,
      description: output.slice(0, 2000),
      category: "system",
      capturedAt: Date.now(),
    } as ToolResult;
  } catch (err) {
    return {
      type: "bashResult",
      title: command,
      description: `命令执行失败: ${err instanceof Error ? err.message : "未知错误"}`,
      category: "system",
      capturedAt: Date.now(),
    } as ToolResult;
  }
};
