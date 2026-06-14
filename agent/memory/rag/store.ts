import { readFileSync, writeFileSync, existsSync, mkdirSync, readdirSync } from "node:fs";
import { resolve, dirname } from "node:path";
import type { RagEntry } from "./types";

const DATA_DIR = resolve(dirname(new URL(import.meta.url).pathname.replace(/^\//, "")), "data");

function ensureDataDir(): void {
  if (!existsSync(DATA_DIR)) {
    mkdirSync(DATA_DIR, { recursive: true });
  }
}

function sanitizeForFilename(s: string): string {
  return s.replace(/[^a-zA-Z0-9_-]/g, "").slice(0, 32);
}

export function list(): RagEntry[] {
  ensureDataDir();
  const files = readdirSync(DATA_DIR).filter((f: string) => f.endsWith(".json"));
  const entries: RagEntry[] = [];
  for (const file of files) {
    try {
      const raw = readFileSync(resolve(DATA_DIR, file), "utf-8");
      entries.push(JSON.parse(raw) as RagEntry);
    } catch {
      // skip corrupt files
    }
  }
  // 按结束时间倒序（最新的在前）
  entries.sort((a, b) => b.endTime.localeCompare(a.endTime));
  return entries;
}

export function save(entry: RagEntry): void {
  ensureDataDir();
  const filename = `${sanitizeForFilename(entry.sessionId)}-${sanitizeForFilename(entry.endTime)}.json`;
  const filepath = resolve(DATA_DIR, filename);
  writeFileSync(filepath, JSON.stringify(entry, null, 2), "utf-8");
}
