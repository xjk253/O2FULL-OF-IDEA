export function filterTts(text: string): string {
  return text
    // 兜底:剥离任何漏过的 think/thinking 块(尖括号+方括号)
    .replace(/<think(?:ing)?\b[^>]*>[\s\S]*?<\/think(?:ing)?\b[^>]*>/gi, "")
    .replace(/<think(?:ing)?\b[^>]*\/>/gi, "")
    .replace(/\[thinking\][\s\S]*?\[\/thinking\]/gi, "")
    .replace(/\[think\][\s\S]*?\[\/think\]/gi, "")
    .replace(/\[[a-zA-Z]+\]/g, "")        // strip emotion tags
    .replace(/\s+/g, " ")                 // collapse whitespace
    .trim();
}
