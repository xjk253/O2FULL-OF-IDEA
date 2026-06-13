export function processDisplay(text: string): string {
  return text
    .replace(/<think\/>/g, "(")
    .replace(/<\/think\/>/g, ")");
}
