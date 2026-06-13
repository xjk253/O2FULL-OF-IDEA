export function filterTts(text: string): string {
  return text
    .replace(/<think\/>.*?<\/think\/>/gs, "") // strip think content
    .replace(/\[[a-zA-Z]+\]/g, "")        // strip emotion tags
    .replace(/\s+/g, " ")                 // collapse whitespace
    .trim();
}
