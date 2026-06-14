export function processDisplay(text: string): string {
  // 兜底:剥离任何漏过来的 think 标签(正常情况 sentenceDivider 已处理)
  return text
    .replace(/<think\b[^>]*>[\s\S]*?<\/think\b[^>]*>/gi, "")
    .replace(/<think\b[^>]*\/>/gi, "")
    .trim();
}
