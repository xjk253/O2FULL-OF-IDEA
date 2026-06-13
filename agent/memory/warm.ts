import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";

const DEFAULT_MAX_CHARS = 2200;
const SECTION_DELIMITER = "§";

export interface WarmMemory {
  entries: string[];
  raw: string;
}

export interface WarmMemoryStore {
  memory: WarmMemory;
  user: WarmMemory;
  load(): void;
  addMemory(entry: string): void;
  addUser(entry: string): void;
  getContext(): string;
}

export function createWarmMemory(baseDir: string): WarmMemoryStore {
  const memDir = resolve(baseDir, "memories");
  const memPath = resolve(memDir, "MEMORY.md");
  const userPath = resolve(memDir, "USER.md");

  let memory = parseFile(memPath);
  let user = parseFile(userPath);

  function load() {
    memory = parseFile(memPath);
    user = parseFile(userPath);
  }

  function addMemory(entry: string) {
    const newEntry = `${SECTION_DELIMITER} ${entry}`;
    if (memory.raw.length + newEntry.length > DEFAULT_MAX_CHARS) {
      // evict oldest section
      const sections = memory.raw.split(SECTION_DELIMITER);
      sections.shift();
      memory.raw = (SECTION_DELIMITER + sections.join(SECTION_DELIMITER)).trim();
    }
    memory.raw = (memory.raw + "\n" + newEntry).trim();
    memory.entries = parseSections(memory.raw);
    writeFileSync(memPath, memory.raw, "utf-8");
  }

  function addUser(entry: string) {
    const newEntry = `${SECTION_DELIMITER} ${entry}`;
    if (user.raw.length + newEntry.length > DEFAULT_MAX_CHARS) {
      const sections = user.raw.split(SECTION_DELIMITER);
      sections.shift();
      user.raw = (SECTION_DELIMITER + sections.join(SECTION_DELIMITER)).trim();
    }
    user.raw = (user.raw + "\n" + newEntry).trim();
    user.entries = parseSections(user.raw);
    writeFileSync(userPath, user.raw, "utf-8");
  }

  function getContext(): string {
    const parts: string[] = [];
    if (memory.entries.length > 0) {
      parts.push("## 泡泡的记忆\n" + memory.entries.map((e) => `- ${e}`).join("\n"));
    }
    if (user.entries.length > 0) {
      parts.push("## 关于主人\n" + user.entries.map((e) => `- ${e}`).join("\n"));
    }
    return parts.join("\n\n");
  }

  return { memory, user, load, addMemory, addUser, getContext };
}

function parseFile(filePath: string): WarmMemory {
  mkdirSync(dirname(filePath), { recursive: true });
  if (!existsSync(filePath)) {
    writeFileSync(filePath, "", "utf-8");
    return { entries: [], raw: "" };
  }
  const raw = readFileSync(filePath, "utf-8");
  return { entries: parseSections(raw), raw };
}

function parseSections(raw: string): string[] {
  return raw
    .split(SECTION_DELIMITER)
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
}
