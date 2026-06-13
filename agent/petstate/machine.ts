import { Mood, type PetState, type StateEvent } from "./types";
import { clampAffinity } from "./affinity";

const DEFAULT_STATE: PetState = {
  mood: Mood.Normal,
  affinity: 50,
  lastActiveTime: 0,
};

function moodAfterEvent(current: Mood, event: StateEvent): Mood {
  switch (event.type) {
    case "userActive":
      return current === Mood.Lonely ? Mood.Normal : Mood.Happy;

    case "conversationEnd":
      return Mood.Normal;

    case "inspirationCaptured":
      return Mood.Inspired;

    case "userGone":
      return Mood.Lonely;
  }
}

function affinityDelta(event: StateEvent): number {
  switch (event.type) {
    case "userActive":
      return 1;
    case "inspirationCaptured":
      return 3;
    case "userGone":
      return -1;
    case "conversationEnd":
      return 0;
  }
}

export class PetStateMachine {
  private states = new Map<string, PetState>();

  getState(sessionId: string): PetState {
    if (!this.states.has(sessionId)) {
      this.states.set(sessionId, { ...DEFAULT_STATE, lastActiveTime: Date.now() });
    }
    return this.states.get(sessionId)!;
  }

  setState(sessionId: string, state: PetState): void {
    this.states.set(sessionId, { ...state });
  }

  transition(sessionId: string, event: StateEvent, now: number): PetState {
    const current = this.getState(sessionId);
    const next: PetState = {
      mood: moodAfterEvent(current.mood, event),
      affinity: clampAffinity(current.affinity + affinityDelta(event)),
      lastActiveTime: now,
    };
    this.states.set(sessionId, next);
    return next;
  }
}
