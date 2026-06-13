export enum Mood {
  Normal = "normal",
  Happy = "happy",
  Inspired = "inspired",
  Lonely = "lonely",
}

export interface PetState {
  mood: Mood;
  affinity: number;
  lastActiveTime: number;
}

export type StateEvent =
  | { type: "userActive" }
  | { type: "conversationEnd" }
  | { type: "inspirationCaptured" }
  | { type: "userGone" };
