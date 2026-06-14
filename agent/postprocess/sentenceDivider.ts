export async function* sentenceDivider(
  tokens: AsyncIterable<string>,
): AsyncIterable<string> {
  let buffer = "";

  for await (const chunk of tokens) {
    buffer += chunk;
  }

  // 统一在分句前剥离所有 think 块,兼容多种写法:
  //   <think>...</think>  <think/>...</think>  <think attr>...</think>
  //   非标准双斜杠:  <think/>...</think/>  (旧测试用例)
  //   自闭合无内容:  <think/>  <think></think>
  buffer = buffer
    .replace(/<think\b[^>]*>[\s\S]*?<\/think\b[^>]*>/gi, "")
    .replace(/<think\b[^>]*\/>/gi, "");

  // split on sentence boundaries, keeping delimiter with sentence
  const parts = buffer.split(/(?<=[。！？.!?])/g);

  for (const part of parts) {
    const trimmed = part.trim();
    if (trimmed.length > 0) {
      yield trimmed;
    }
  }
}
