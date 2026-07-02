function breakAtLastSpace(text, maxLength, searchStart) {
  let chunk = text.slice(0, maxLength);
  const zone = chunk.slice(searchStart);
  const lastSpace = zone.lastIndexOf(' ');
  if (lastSpace !== -1) {
    chunk = chunk.slice(0, searchStart + lastSpace);
  }
  return chunk;
}

function splitString(text, l1, l2, l3) {
  const result = [];
  let remaining = text;
  if (remaining.length <= l1) {
    result.push(remaining);
    return result;
  }
  result.push(breakAtLastSpace(remaining, l1, Math.max(0, l1 - l3)));
  remaining = remaining.slice(result[0].length).trimStart();
  while (remaining.length > 0) {
    if (remaining.length <= l2) {
      result.push(remaining);
      break;
    }
    const chunk = breakAtLastSpace(remaining, l2, 0);
    result.push(chunk);
    remaining = remaining.slice(chunk.length).trimStart();
  }
  return result;
}

module.exports = splitString;
