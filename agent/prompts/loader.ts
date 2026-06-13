import { readFileSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

// cache templates after first load
const cache = new Map<string, string>();

const baseDir = resolve(dirname(fileURLToPath(import.meta.url)), "templates");

export function loadTemplate(filename: string): string {
  if (cache.has(filename)) return cache.get(filename)!;

  const path = resolve(baseDir, filename);
  const raw = readFileSync(path, "utf-8");
  cache.set(filename, raw);
  return raw;
}

export function renderTemplate(
  filename: string,
  vars: Record<string, string>,
): string {
  let tpl = loadTemplate(filename);
  for (const [key, value] of Object.entries(vars)) {
    tpl = tpl.replace(new RegExp(`\\{\\{${key}\\}\\}`, "g"), value);
  }
  return tpl;
}
