export async function* sentenceDivider(
  tokens: AsyncIterable<string>,
): AsyncIterable<string> {
  let buffer = "";

  for await (const chunk of tokens) {
    buffer += chunk;
  }

  // 统一在分句前剥离所有 think/thinking 块,兼容多种写法:
  //   尖括号: <think>...</think>  <thinking>...</thinking>
  //   自闭合: <think/>  <thinking/>
  //   方括号: [thinking]...[/thinking]  (LLM 偶尔用这种)
  buffer = buffer
    .replace(/<think(?:ing)?\b[^>]*>[\s\S]*?<\/think(?:ing)?\b[^>]*>/gi, "")
    .replace(/<think(?:ing)?\b[^>]*\/>/gi, "")
    .replace(/\[thinking\][\s\S]*?\[\/thinking\]/gi, "")
    .replace(/\[think\][\s\S]*?\[\/think\]/gi, "");

  // split on sentence boundaries, keeping delimiter with sentence
  const parts = buffer.split(/(?<=[。！？.!?])/g);

  for (const part of parts) {
    const trimmed = part.trim();
    if (trimmed.length > 0) {
      yield trimmed;
    }
  }
}
