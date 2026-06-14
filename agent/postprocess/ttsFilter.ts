export function filterTts(text: string): string {
  return text
    // 兜底:剥离任何漏过的 think 块(正常情况 sentenceDivider 已处理)
    .replace(/<think\b[^>]*>[\s\S]*?<\/think\b[^>]*>/gi, "")
    .replace(/<think\b[^>]*\/>/gi, "")
    .replace(/\[[a-zA-Z]+\]/g, "")        // strip emotion tags
    .replace(/\s+/g, " ")                 // collapse whitespace
    .trim();
}
