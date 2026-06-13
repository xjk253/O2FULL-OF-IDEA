import { createWarmMemory, type WarmMemoryStore } from "./warm";

let store: WarmMemoryStore | null = null;

export function getRetriever(baseDir: string): WarmMemoryStore {
  if (!store) {
    store = createWarmMemory(baseDir);
  }
  return store;
}
