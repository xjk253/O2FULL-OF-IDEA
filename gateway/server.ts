import "dotenv/config";
import { WebSocketServer, WebSocket } from "ws";
import { loadCharacter } from "../agent/character/loader";
import { loadEmoMap } from "../agent/emoMap/loader";
import { PetStateMachine } from "../agent/petstate/machine";
import { buildPrompt } from "../agent/prompts/factory";
import { preprocess } from "../agent/preprocess/preprocess";
import { runLoop } from "../agent/loop/loop";
import { composePipeline } from "../agent/postprocess/compose";
import { createTools } from "../agent/tools";
import { checkProactive, toSentenceOutput } from "../agent/triggers/proactive";
import { createWarmMemory } from "../agent/memory/warm";
import { save as saveL1, getRecent } from "../agent/memory/l1";
import type { ToolContext } from "../agent/tools/types";

const PORT = 8080;

const character = loadCharacter("../agent/character/config.yaml");
const emoMap = loadEmoMap("../agent/emoMap/modelDict.json");
const postprocess = composePipeline(emoMap);
const warmMemory = createWarmMemory("../../.bubble");

const stateMachines = new Map<string, PetStateMachine>();

function getMachine(sessionId: string): PetStateMachine {
  if (!stateMachines.has(sessionId)) {
    stateMachines.set(sessionId, new PetStateMachine());
  }
  return stateMachines.get(sessionId)!;
}

async function handleChat(sessionId: string, text: string, send: (msg: object) => void) {
  const stateMachine = getMachine(sessionId);
  const state = stateMachine.getState(sessionId);
  console.log(`[chat] 收到消息: "${text}" | 心情:${state.mood} 亲密度:${state.affinity}`);

  const toolCtx: ToolContext = {
    character: { name: character.name },
    petState: { mood: state.mood, affinity: state.affinity },
    sessionId,
  };

  const tools = createTools(toolCtx);

  const result = preprocess(
    { text, sessionId },
    {
      stateMachine,
      trigger: { check(s) { return checkProactive(s); } },
      memory: { getRecent: (sid, n) => getRecent(sid, n) },
      promptFactory: { build: buildPrompt },
      character,
      emoMap,
      tools,
      warmMemory,
    },
  );

  if (result.kind === "shortCircuit") {
    console.log(`[preprocess] ⚡ 短路触发:`, result.output);
    const out = toSentenceOutput(result.output as any);
    send({ type: "sentence", text: out.ttsText, expression: "sad" });
    send({ type: "done" });
    return;
  }

  console.log(`[preprocess] ✅ 进入LLM | prompt长度:${result.systemPrompt.length}字 历史消息:${result.messages.length}条`);

  const tokens = runLoop(
    { systemPrompt: result.systemPrompt, messages: result.messages, tools },
    {},
    toolCtx,
  );

  let fullText = "";
  for await (const token of tokens) {
    fullText += token;
  }
  console.log(`[LLM] 原始回复: "${fullText}"`);

  if (fullText) {
    saveL1(sessionId, text, fullText);
    console.log(`[memory] 记忆已保存 (${text.length + fullText.length}字)`);
  }

  async function* singleChunk() { yield fullText; }

  let sentenceCount = 0;
  for await (const s of postprocess(singleChunk())) {
    const exprName = Object.entries(emoMap.emotionMap).find(
      ([, id]) => id === s.actions.expressions[0],
    )?.[0];
    sentenceCount++;
    console.log(`[postprocess] 句子${sentenceCount}: "${s.ttsText}" 表情:${exprName ?? "无"}`);

    send({
      type: "sentence",
      text: s.ttsText,
      expression: exprName ?? null,
      display: s.displayText,
    });
  }

  console.log(`[done] ✅ 回复完成 (共${sentenceCount}句)`);
  send({ type: "done" });
}

const wss = new WebSocketServer({ port: PORT });

wss.on("connection", (ws: WebSocket) => {
  let sessionId = `ws-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
  console.log(`[gateway] 新连接: ${sessionId}`);

  ws.on("message", async (raw) => {
    try {
      const msg = JSON.parse(raw.toString());
      // 客户端可以指定持久化 sessionId,以保留跨连接的会话历史
      if (msg.type === "hello_ack" && typeof msg.sessionId === "string") {
        const old = sessionId;
        sessionId = msg.sessionId;
        console.log(`[gateway] 会话 ID 切换: ${old} -> ${sessionId}`);
        return;
      }
      if (msg.type === "chat") {
        await handleChat(sessionId, msg.text, (obj) => {
          ws.send(JSON.stringify(obj));
        });
      }
    } catch (err) {
      ws.send(JSON.stringify({ type: "error", message: String(err) }));
    }
  });

  ws.on("close", () => {
    console.log(`[gateway] 断开: ${sessionId}`);
  });

  ws.send(JSON.stringify({ type: "hello", sessionId }));
});

console.log(`[gateway] 泡泡网关已启动 ws://localhost:${PORT}`);
