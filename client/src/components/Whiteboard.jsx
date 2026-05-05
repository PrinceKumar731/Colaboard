import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import Toolbar from './Toolbar';
import { replayHistory } from '../utils/canvas';

const SHAPE_TOOLS = new Set(['line', 'arrow', 'rectangle', 'ellipse', 'diamond']);
const MIN_ZOOM = 0.35;
const MAX_ZOOM = 3;
const ZOOM_STEP = 1.12;
const DEFAULT_VIEWPORT = { x: 260, y: 180, zoom: 1 };

function createElementId() {
  return `el-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function withAlpha(hex, alpha) {
  return `${hex}${Math.round(alpha * 255).toString(16).padStart(2, '0')}`;
}

function createWsUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.hostname}:8080/ws`;
}

export default function Whiteboard({ roomId }) {
  const canvasRef = useRef(null);
  const canvasAreaRef = useRef(null);
  const stompRef = useRef(null);
  const ctxRef = useRef(null);
  const pixelRatioRef = useRef(window.devicePixelRatio || 1);
  const draftRef = useRef(null);
  const elementsRef = useRef([]);
  const isDrawingRef = useRef(false);
  const isPanningRef = useRef(false);
  const panOriginRef = useRef({ x: 0, y: 0, startX: 0, startY: 0 });
  const mySessionRef = useRef(null);
  const viewportRef = useRef(DEFAULT_VIEWPORT);
  const spacePressedRef = useRef(false);

  const [tool, setTool] = useState('pen');
  const [color, setColor] = useState('#1f2937');
  const [fillStyle, setFillStyle] = useState('transparent');
  const [lineWidth, setLineWidth] = useState(3);
  const [userCount, setUserCount] = useState(1);
  const [cursors, setCursors] = useState({});
  const [copied, setCopied] = useState(false);
  const [connected, setConnected] = useState(false);
  const [shareOpen, setShareOpen] = useState(false);
  const [viewport, setViewport] = useState(DEFAULT_VIEWPORT);
  const [isPanning, setIsPanning] = useState(false);

  const renderScene = useCallback((previewElement = draftRef.current) => {
    const canvas = canvasRef.current;
    const ctx = ctxRef.current;
    if (!canvas || !ctx) return;

    const ratio = pixelRatioRef.current;
    const { x, y, zoom } = viewportRef.current;

    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.setTransform(ratio * zoom, 0, 0, ratio * zoom, ratio * x, ratio * y);
    replayHistory(ctx, elementsRef.current, previewElement);
  }, []);

  const updateViewport = useCallback((nextViewport) => {
    viewportRef.current = nextViewport;
    setViewport(nextViewport);
    renderScene();
  }, [renderScene]);

  const resizeCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    const container = canvasAreaRef.current;
    if (!canvas || !container) return;

    const ratio = window.devicePixelRatio || 1;
    pixelRatioRef.current = ratio;

    canvas.width = Math.floor(container.clientWidth * ratio);
    canvas.height = Math.floor(container.clientHeight * ratio);
    canvas.style.width = `${container.clientWidth}px`;
    canvas.style.height = `${container.clientHeight}px`;

    ctxRef.current = canvas.getContext('2d');
    renderScene();
  }, [renderScene]);

  useEffect(() => {
    resizeCanvas();
    const observer = new ResizeObserver(() => resizeCanvas());
    if (canvasAreaRef.current) observer.observe(canvasAreaRef.current);
    window.addEventListener('resize', resizeCanvas);

    return () => {
      observer.disconnect();
      window.removeEventListener('resize', resizeCanvas);
    };
  }, [resizeCanvas]);

  useEffect(() => {
    const client = new Client({
      brokerURL: createWsUrl(),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);

        client.subscribe('/user/queue/session', (message) => {
          mySessionRef.current = message.body;
        });

        client.subscribe('/user/queue/canvas-state', (message) => {
          elementsRef.current = JSON.parse(message.body);
          renderScene(null);
        });

        client.subscribe(`/topic/room/${roomId}/draw`, (message) => {
          const element = JSON.parse(message.body);
          if (elementsRef.current.some((item) => item.id === element.id)) return;
          elementsRef.current = [...elementsRef.current, element];
          renderScene(null);
        });

        client.subscribe(`/topic/room/${roomId}/cursor`, (message) => {
          const { sessionId, x, y } = JSON.parse(message.body);
          if (sessionId === mySessionRef.current) return;
          setCursors((current) => ({ ...current, [sessionId]: { x, y } }));
        });

        client.subscribe(`/topic/room/${roomId}/cursor-leave`, (message) => {
          const sessionId = message.body;
          setCursors((current) => {
            const next = { ...current };
            delete next[sessionId];
            return next;
          });
        });

        client.subscribe(`/topic/room/${roomId}/users`, (message) => {
          setUserCount(Number(message.body));
        });

        client.subscribe(`/topic/room/${roomId}/clear`, () => {
          elementsRef.current = [];
          draftRef.current = null;
          renderScene(null);
        });

        client.publish({ destination: `/app/room/${roomId}/join`, body: '' });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => console.error('STOMP error', frame),
    });

    client.activate();
    stompRef.current = client;

    return () => client.deactivate();
  }, [renderScene, roomId]);

  useEffect(() => {
    const keyMap = {
      p: 'pen',
      l: 'line',
      a: 'arrow',
      r: 'rectangle',
      e: 'ellipse',
      d: 'diamond',
      x: 'eraser',
    };

    const onKeyDown = (event) => {
      if (event.target instanceof HTMLInputElement) return;
      if (event.code === 'Space') {
        event.preventDefault();
        spacePressedRef.current = true;
        return;
      }

      const nextTool = keyMap[event.key.toLowerCase()];
      if (nextTool) setTool(nextTool);
    };

    const onKeyUp = (event) => {
      if (event.code === 'Space') {
        spacePressedRef.current = false;
        setIsPanning(false);
      }
    };

    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);

    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
    };
  }, []);

  const getWorldPosition = (event) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const { x, y, zoom } = viewportRef.current;
    return {
      x: (event.clientX - rect.left - x) / zoom,
      y: (event.clientY - rect.top - y) / zoom,
    };
  };

  const toScreenPosition = (point) => {
    const { x, y, zoom } = viewportRef.current;
    return {
      left: point.x * zoom + x,
      top: point.y * zoom + y,
    };
  };

  const publishCursor = (position) => {
    if (!stompRef.current?.connected) return;
    stompRef.current.publish({
      destination: `/app/room/${roomId}/cursor`,
      body: JSON.stringify(position),
    });
  };

  const buildDraftElement = (position) => {
    const id = createElementId();
    const fillColor = fillStyle === 'solid' && tool !== 'line' && tool !== 'arrow'
      ? withAlpha(color, 0.18)
      : null;

    if (tool === 'pen' || tool === 'eraser') {
      return {
        id,
        elementType: 'stroke',
        tool,
        color,
        lineWidth,
        points: [position.x, position.y, position.x, position.y],
        startX: position.x,
        startY: position.y,
        endX: position.x,
        endY: position.y,
      };
    }

    return {
      id,
      elementType: 'shape',
      tool,
      color,
      fillColor,
      lineWidth,
      startX: position.x,
      startY: position.y,
      endX: position.x,
      endY: position.y,
    };
  };

  const zoomAtPoint = useCallback((clientX, clientY, factor) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const pointX = clientX - rect.left;
    const pointY = clientY - rect.top;
    const nextZoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, viewportRef.current.zoom * factor));
    const worldX = (pointX - viewportRef.current.x) / viewportRef.current.zoom;
    const worldY = (pointY - viewportRef.current.y) / viewportRef.current.zoom;

    updateViewport({
      zoom: nextZoom,
      x: pointX - worldX * nextZoom,
      y: pointY - worldY * nextZoom,
    });
  }, [updateViewport]);

  const handlePointerDown = (event) => {
    event.preventDefault();

    if (event.button === 1 || spacePressedRef.current) {
      isPanningRef.current = true;
      setIsPanning(true);
      panOriginRef.current = {
        x: event.clientX,
        y: event.clientY,
        startX: viewportRef.current.x,
        startY: viewportRef.current.y,
      };
      event.currentTarget.setPointerCapture(event.pointerId);
      return;
    }

    const position = getWorldPosition(event);
    isDrawingRef.current = true;
    draftRef.current = buildDraftElement(position);
    publishCursor(position);
    renderScene();
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event) => {
    if (isPanningRef.current) {
      updateViewport({
        ...viewportRef.current,
        x: panOriginRef.current.startX + (event.clientX - panOriginRef.current.x),
        y: panOriginRef.current.startY + (event.clientY - panOriginRef.current.y),
      });
      return;
    }

    const position = getWorldPosition(event);
    publishCursor(position);

    if (!isDrawingRef.current || !draftRef.current) return;

    if (draftRef.current.elementType === 'stroke') {
      draftRef.current = {
        ...draftRef.current,
        endX: position.x,
        endY: position.y,
        points: [...draftRef.current.points, position.x, position.y],
      };
    } else {
      draftRef.current = {
        ...draftRef.current,
        endX: position.x,
        endY: position.y,
      };
    }

    renderScene();
  };

  const finalizeDraft = () => {
    if (!draftRef.current) return;
    const finishedElement = draftRef.current;
    draftRef.current = null;
    isDrawingRef.current = false;

    if (
      finishedElement.elementType === 'stroke' &&
      (!finishedElement.points || finishedElement.points.length < 4)
    ) {
      renderScene(null);
      return;
    }

    if (
      SHAPE_TOOLS.has(finishedElement.tool) &&
      finishedElement.startX === finishedElement.endX &&
      finishedElement.startY === finishedElement.endY
    ) {
      renderScene(null);
      return;
    }

    elementsRef.current = [...elementsRef.current, finishedElement];
    renderScene(null);

    if (stompRef.current?.connected) {
      stompRef.current.publish({
        destination: `/app/room/${roomId}/draw`,
        body: JSON.stringify(finishedElement),
      });
    }
  };

  const handlePointerUp = () => {
    if (isPanningRef.current) {
      isPanningRef.current = false;
      setIsPanning(false);
      return;
    }

    finalizeDraft();
  };

  const handleWheel = (event) => {
    event.preventDefault();

    if (event.ctrlKey || event.metaKey) {
      zoomAtPoint(event.clientX, event.clientY, event.deltaY < 0 ? ZOOM_STEP : 1 / ZOOM_STEP);
      return;
    }

    updateViewport({
      ...viewportRef.current,
      x: viewportRef.current.x - event.deltaX,
      y: viewportRef.current.y - event.deltaY,
    });
  };

  const handleClear = () => {
    elementsRef.current = [];
    draftRef.current = null;
    renderScene(null);
    if (stompRef.current?.connected) {
      stompRef.current.publish({ destination: `/app/room/${roomId}/clear`, body: '' });
    }
  };

  const handleExport = () => {
    const canvas = canvasRef.current;
    const exportCanvas = document.createElement('canvas');
    exportCanvas.width = canvas.width;
    exportCanvas.height = canvas.height;
    const exportCtx = exportCanvas.getContext('2d');
    exportCtx.fillStyle = '#fff8ec';
    exportCtx.fillRect(0, 0, exportCanvas.width, exportCanvas.height);
    exportCtx.drawImage(canvas, 0, 0);

    const link = document.createElement('a');
    link.href = exportCanvas.toDataURL('image/png');
    link.download = `colaboard-${roomId}.png`;
    link.click();
  };

  const copyLink = async () => {
    await navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  const centerViewport = () => {
    updateViewport(DEFAULT_VIEWPORT);
  };

  const stepZoom = (direction) => {
    const container = canvasAreaRef.current;
    if (!container) return;
    const factor = direction > 0 ? ZOOM_STEP : 1 / ZOOM_STEP;
    zoomAtPoint(
      container.getBoundingClientRect().left + container.clientWidth / 2,
      container.getBoundingClientRect().top + container.clientHeight / 2,
      factor,
    );
  };

  return (
    <div className="board-shell">
      <header className="board-header">
        <div className="board-header-left">
          <button type="button" className="header-icon-btn" aria-label="Menu">
            ☰
          </button>
          <div className="brand-chip">Colaboard</div>
        </div>

        <div className="header-actions">
          <div className={`status-dot${connected ? ' online' : ''}`} title={connected ? 'Live sync on' : 'Connecting'} />
          <button type="button" className="share-btn" onClick={() => setShareOpen(true)}>
            Share
          </button>
        </div>
      </header>

      <main className="board-main">
        <Toolbar
          tool={tool}
          color={color}
          fillStyle={fillStyle}
          lineWidth={lineWidth}
          onToolChange={setTool}
          onColorChange={setColor}
          onFillStyleChange={setFillStyle}
          onLineWidthChange={setLineWidth}
          onClear={handleClear}
          onExport={handleExport}
        />

        <section className="canvas-stage-wrap">
          <div className="canvas-stage" ref={canvasAreaRef}>
            <div className="canvas-topbar">
              <div className="viewport-controls">
                <button type="button" onClick={() => stepZoom(-1)}>-</button>
                <strong>{Math.round(viewport.zoom * 100)}%</strong>
                <button type="button" onClick={() => stepZoom(1)}>+</button>
                <button type="button" className="viewport-reset" onClick={centerViewport}>Reset</button>
              </div>
            </div>

            <div
              className="grid-layer"
              style={{
                backgroundSize: `${28 * viewport.zoom}px ${28 * viewport.zoom}px`,
                backgroundPosition: `${viewport.x}px ${viewport.y}px`,
              }}
            />

            <canvas
              ref={canvasRef}
              className="canvas"
              onPointerDown={handlePointerDown}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              onPointerCancel={handlePointerUp}
              onPointerLeave={handlePointerUp}
              onWheel={handleWheel}
              style={{
                cursor: isPanning || spacePressedRef.current
                  ? 'grab'
                  : tool === 'eraser'
                    ? 'cell'
                    : 'crosshair',
              }}
            />

            {Object.entries(cursors).map(([id, position]) => (
              <div
                key={id}
                className="remote-cursor"
                style={toScreenPosition(position)}
              >
                <span />
              </div>
            ))}
          </div>
        </section>
      </main>

      {shareOpen && (
        <div className="modal-backdrop" onClick={() => setShareOpen(false)}>
          <div className="share-modal" onClick={(event) => event.stopPropagation()}>
            <div className="share-modal-head">
              <div>
                <h2>Start a live session</h2>
                <p>Share this link so others can join your board instantly.</p>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setShareOpen(false)}
                aria-label="Close"
              >
                ✕
              </button>
            </div>

            <div className="share-room-badge">
              <span>Room</span>
              <strong>{roomId}</strong>
              <em>{userCount} collaborator{userCount === 1 ? '' : 's'}</em>
            </div>

            <div className="share-link-box">{window.location.href}</div>

            <div className="share-modal-actions">
              <button
                type="button"
                className="share-btn primary"
                onClick={copyLink}
              >
                {copied ? 'Copied' : 'Copy link'}
              </button>
              <button
                type="button"
                className="ghost-btn"
                onClick={() => setShareOpen(false)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
