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

const PORT = parseInt(process.env.PORT || "8080", 10);

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
    const out = toSentenceOutput(result.output as any);
    send({ type: "sentence", text: out.ttsText, expression: "sad" });
    send({ type: "done" });
    return;
  }

  const tokens = runLoop(
    { systemPrompt: result.systemPrompt, messages: result.messages, tools },
    {},
    toolCtx,
  );

  let fullText = "";
  for await (const token of tokens) {
    fullText += token;
  }

  if (fullText) {
    saveL1(sessionId, text, fullText);
  }

  async function* singleChunk() { yield fullText; }

  for await (const s of postprocess(singleChunk())) {
    const exprName = Object.entries(emoMap.emotionMap).find(
      ([, id]) => id === s.actions.expressions[0],
    )?.[0];

    send({
      type: "sentence",
      text: s.ttsText,
      expression: exprName ?? null,
      display: s.displayText,
    });
  }

  send({ type: "done" });
}

const wss = new WebSocketServer({ port: PORT });

wss.on("connection", (ws: WebSocket) => {
  const sessionId = `ws-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
  console.log(`[gateway] 新连接: ${sessionId}`);

  ws.on("message", async (raw) => {
    try {
      const msg = JSON.parse(raw.toString());
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
