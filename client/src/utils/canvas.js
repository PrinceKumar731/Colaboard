export function drawSegment(ctx, segment) {
  ctx.beginPath();
  ctx.strokeStyle = segment.tool === 'eraser' ? '#ffffff' : segment.color;
  ctx.lineWidth = segment.lineWidth;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.moveTo(segment.prevX, segment.prevY);
  ctx.lineTo(segment.x, segment.y);
  ctx.stroke();
}

export function replayHistory(ctx, history) {
  ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
  history.forEach((seg) => drawSegment(ctx, seg));
}
