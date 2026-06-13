export async function* sentenceDivider(
  tokens: AsyncIterable<string>,
): AsyncIterable<string> {
  let buffer = "";

  for await (const chunk of tokens) {
    buffer += chunk;
  }

  // split on sentence boundaries, keeping delimiter with sentence
  // but NOT inside <think/> tags
  const parts = buffer.split(/(?<=[。！？.!?])/g);

  let inside = false;
  let acc = "";

  for (const part of parts) {
    const hasOpen = part.includes("<think/>");
    const hasClose = part.includes("</think/>");

    if (hasOpen && hasClose) {
      // single sentence think block
      acc += part;
      yield acc.trim();
      acc = "";
    } else if (hasOpen) {
      inside = true;
      acc += part;
    } else if (hasClose) {
      inside = false;
      acc += part;
      yield acc.trim();
      acc = "";
    } else if (inside) {
      acc += part;
    } else {
      const trimmed = part.trim();
      if (trimmed.length > 0) {
        yield trimmed;
      }
    }
  }

  if (acc.trim().length > 0) {
    yield acc.trim();
  }
}
