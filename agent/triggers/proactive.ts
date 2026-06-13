import { Mood, type PetState } from "../petstate/types";

interface TriggerResult {
  type: "proactiveSpeak";
  message: string;
  expression: string;
}

const LONELY_IDLE_MS = 4 * 3600_000;

const LONELY_MESSAGES = [
  "主人……你是不是把我忘了……",
  "呜……主人好久没来陪泡泡了……",
  "主人你终于来了！泡泡等了你好久……",
];

function pickLonelyMessage(): string {
  return LONELY_MESSAGES[Math.floor(Math.random() * LONELY_MESSAGES.length)];
}

export function checkProactive(state: PetState): TriggerResult | null {
  const idle = Date.now() - state.lastActiveTime;

  // lonely: idle > 4h and not already lonely
  if (idle > LONELY_IDLE_MS && state.mood !== Mood.Lonely) {
    return {
      type: "proactiveSpeak",
      message: pickLonelyMessage(),
      expression: "sad",
    };
  }

  return null;
}

export function toSentenceOutput(result: TriggerResult) {
  return {
    displayText: `[${result.expression}] ${result.message}`,
    ttsText: result.message,
    actions: { expressions: [] },
  };
}
