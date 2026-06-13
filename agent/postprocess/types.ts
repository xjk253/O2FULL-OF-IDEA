export interface Actions {
  expressions: number[];
}

export interface SentenceOutput {
  displayText: string;
  ttsText: string;
  actions: Actions;
}
