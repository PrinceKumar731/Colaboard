function withStyle(ctx, element, draw) {
  ctx.save();
  ctx.lineWidth = element.lineWidth || 2;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';
  ctx.strokeStyle = element.color || '#1f2937';
  ctx.fillStyle = element.fillColor || 'transparent';
  ctx.globalCompositeOperation = element.tool === 'eraser' ? 'destination-out' : 'source-over';
  draw();
  ctx.restore();
}

function getBounds(element) {
  const startX = element.startX ?? element.prevX ?? 0;
  const startY = element.startY ?? element.prevY ?? 0;
  const endX = element.endX ?? element.x ?? startX;
  const endY = element.endY ?? element.y ?? startY;
  return {
    startX,
    startY,
    endX,
    endY,
    width: endX - startX,
    height: endY - startY,
  };
}

function strokePath(ctx, points) {
  if (!points?.length) return;
  ctx.beginPath();
  ctx.moveTo(points[0], points[1]);

  for (let i = 2; i < points.length; i += 2) {
    const x = points[i];
    const y = points[i + 1];
    ctx.lineTo(x, y);
  }

  ctx.stroke();
}

function drawArrowHead(ctx, fromX, fromY, toX, toY, lineWidth) {
  const headLength = Math.max(14, lineWidth * 3);
  const angle = Math.atan2(toY - fromY, toX - fromX);

  ctx.beginPath();
  ctx.moveTo(toX, toY);
  ctx.lineTo(
    toX - headLength * Math.cos(angle - Math.PI / 7),
    toY - headLength * Math.sin(angle - Math.PI / 7),
  );
  ctx.moveTo(toX, toY);
  ctx.lineTo(
    toX - headLength * Math.cos(angle + Math.PI / 7),
    toY - headLength * Math.sin(angle + Math.PI / 7),
  );
  ctx.stroke();
}

function drawRectangle(ctx, element) {
  const { startX, startY, width, height } = getBounds(element);
  ctx.beginPath();
  ctx.rect(startX, startY, width, height);
  if (element.fillColor) ctx.fill();
  ctx.stroke();
}

function drawDiamond(ctx, element) {
  const { startX, startY, endX, endY } = getBounds(element);
  const centerX = (startX + endX) / 2;
  const centerY = (startY + endY) / 2;

  ctx.beginPath();
  ctx.moveTo(centerX, startY);
  ctx.lineTo(endX, centerY);
  ctx.lineTo(centerX, endY);
  ctx.lineTo(startX, centerY);
  ctx.closePath();
  if (element.fillColor) ctx.fill();
  ctx.stroke();
}

function drawEllipse(ctx, element) {
  const { startX, startY, width, height } = getBounds(element);
  ctx.beginPath();
  ctx.ellipse(
    startX + width / 2,
    startY + height / 2,
    Math.abs(width) / 2,
    Math.abs(height) / 2,
    0,
    0,
    Math.PI * 2,
  );
  if (element.fillColor) ctx.fill();
  ctx.stroke();
}

function drawLine(ctx, element) {
  const { startX, startY, endX, endY } = getBounds(element);
  ctx.beginPath();
  ctx.moveTo(startX, startY);
  ctx.lineTo(endX, endY);
  ctx.stroke();

  if (element.tool === 'arrow') {
    drawArrowHead(ctx, startX, startY, endX, endY, element.lineWidth || 2);
  }
}

export function drawSegment(ctx, element) {
  withStyle(ctx, element, () => {
    if (element.elementType === 'stroke' || element.tool === 'pen' || element.tool === 'eraser') {
      const points = element.points?.length
        ? element.points
        : [element.prevX, element.prevY, element.x, element.y];
      strokePath(ctx, points);
      return;
    }

    if (element.tool === 'rectangle') {
      drawRectangle(ctx, element);
      return;
    }

    if (element.tool === 'diamond') {
      drawDiamond(ctx, element);
      return;
    }

    if (element.tool === 'ellipse') {
      drawEllipse(ctx, element);
      return;
    }

    drawLine(ctx, element);
  });
}

export function replayHistory(ctx, history, previewElement = null) {
  history.forEach((element) => drawSegment(ctx, element));
  if (previewElement) drawSegment(ctx, previewElement);
}
