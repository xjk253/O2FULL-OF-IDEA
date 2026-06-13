import { describe, it, expect, beforeEach } from "vitest";
import { PetStateMachine } from "./machine";
import { Mood } from "./types";

let machine: PetStateMachine;

beforeEach(() => {
  machine = new PetStateMachine();
});

function now() {
  return Date.now();
}

describe("PetStateMachine", () => {
  it("new session starts with normal mood and affinity 50", () => {
    const state = machine.getState("s1");
    expect(state.mood).toBe(Mood.Normal);
    expect(state.affinity).toBe(50);
  });

  it("userActive: normal → happy, affinity +1", () => {
    machine.getState("s1");
    const next = machine.transition("s1", { type: "userActive" }, now());
    expect(next.mood).toBe(Mood.Happy);
    expect(next.affinity).toBe(51);
  });

  it("userActive: lonely → normal (not directly happy)", () => {
    machine.setState("s1", { mood: Mood.Lonely, affinity: 30, lastActiveTime: now() - 5 * 3600_000 });
    const next = machine.transition("s1", { type: "userActive" }, now());
    expect(next.mood).toBe(Mood.Normal);
    expect(next.affinity).toBe(31);
  });

  it("conversationEnd: happy → normal", () => {
    machine.setState("s1", { mood: Mood.Happy, affinity: 60, lastActiveTime: now() });
    const next = machine.transition("s1", { type: "conversationEnd" }, now());
    expect(next.mood).toBe(Mood.Normal);
  });

  it("conversationEnd: inspired → normal", () => {
    machine.setState("s1", { mood: Mood.Inspired, affinity: 60, lastActiveTime: now() });
    const next = machine.transition("s1", { type: "conversationEnd" }, now());
    expect(next.mood).toBe(Mood.Normal);
  });

  it("inspirationCaptured: normal → inspired, affinity +3", () => {
    machine.getState("s1");
    const next = machine.transition("s1", { type: "inspirationCaptured" }, now());
    expect(next.mood).toBe(Mood.Inspired);
    expect(next.affinity).toBe(53);
  });

  it("userGone: any → lonely", () => {
    machine.setState("s1", { mood: Mood.Happy, affinity: 60, lastActiveTime: now() });
    const next = machine.transition("s1", { type: "userGone" }, now());
    expect(next.mood).toBe(Mood.Lonely);
  });

  it("affinity clamps at 100", () => {
    machine.setState("s1", { mood: Mood.Normal, affinity: 99, lastActiveTime: now() });
    const next = machine.transition("s1", { type: "inspirationCaptured" }, now());
    expect(next.affinity).toBe(100);
  });

  it("affinity clamps at 0", () => {
    machine.setState("s1", { mood: Mood.Normal, affinity: 0, lastActiveTime: now() });
    const next = machine.transition("s1", { type: "userGone" }, now());
    expect(next.affinity).toBe(0);
  });

  it("getState returns same instance after transition", () => {
    machine.getState("s1");
    machine.transition("s1", { type: "userActive" }, now());
    const state = machine.getState("s1");
    expect(state.mood).toBe(Mood.Happy);
  });
});
