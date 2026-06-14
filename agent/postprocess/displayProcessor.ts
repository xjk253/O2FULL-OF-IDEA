export function processDisplay(text: string): string {
  // 兜底:剥离任何漏过来的 think/thinking 标签
  return text
    .replace(/<think(?:ing)?\b[^>]*>[\s\S]*?<\/think(?:ing)?\b[^>]*>/gi, "")
    .replace(/<think(?:ing)?\b[^>]*\/>/gi, "")
    .replace(/\[thinking\][\s\S]*?\[\/thinking\]/gi, "")
    .replace(/\[think\][\s\S]*?\[\/think\]/gi, "")
    .trim();
}
