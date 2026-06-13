export interface EmoMap {
  name: string;
  emotionMap: Record<string, number>;
}

export type EmotionKeyword = keyof EmoMap["emotionMap"];
export type ExpressionId = number;
