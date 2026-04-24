import { useEffect, useRef, useState } from 'react';
import { io } from 'socket.io-client';
import Toolbar from './Toolbar';
import { drawSegment, replayHistory } from '../utils/canvas';

export default function Whiteboard({ roomId }) {
  const canvasRef = useRef(null);
  const socketRef = useRef(null);
  const isDrawingRef = useRef(false);
  const prevPosRef = useRef(null);
  const ctxRef = useRef(null);

  const [tool, setTool] = useState('pen');
  const [color, setColor] = useState('#000000');
  const [lineWidth, setLineWidth] = useState(4);
  const [userCount, setUserCount] = useState(1);
  const [cursors, setCursors] = useState({});
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = canvas.parentElement;
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;
    ctxRef.current = canvas.getContext('2d');
  }, []);

  useEffect(() => {
    const socket = io();
    socketRef.current = socket;

    socket.emit('join-room', roomId);

    socket.on('canvas-state', (history) => {
      if (ctxRef.current) replayHistory(ctxRef.current, history);
    });

    socket.on('draw', (segment) => {
      if (ctxRef.current) drawSegment(ctxRef.current, segment);
    });

    socket.on('clear', () => {
      const canvas = canvasRef.current;
      if (ctxRef.current) ctxRef.current.clearRect(0, 0, canvas.width, canvas.height);
    });

    socket.on('cursor', ({ id, x, y }) => {
      setCursors((prev) => ({ ...prev, [id]: { x, y } }));
    });

    socket.on('cursor-leave', (id) => {
      setCursors((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
    });

    socket.on('room-users', setUserCount);

    return () => socket.disconnect();
  }, [roomId]);

  const getPos = (e) => {
    const rect = canvasRef.current.getBoundingClientRect();
    const src = e.touches ? e.touches[0] : e;
    return { x: src.clientX - rect.left, y: src.clientY - rect.top };
  };

  const handleMouseDown = (e) => {
    e.preventDefault();
    isDrawingRef.current = true;
    prevPosRef.current = getPos(e);
  };

  const handleMouseMove = (e) => {
    e.preventDefault();
    const pos = getPos(e);
    socketRef.current?.emit('cursor', { roomId, x: pos.x, y: pos.y });

    if (!isDrawingRef.current || !prevPosRef.current) return;

    const segment = {
      prevX: prevPosRef.current.x,
      prevY: prevPosRef.current.y,
      x: pos.x,
      y: pos.y,
      color,
      lineWidth,
      tool
    };

    drawSegment(ctxRef.current, segment);
    socketRef.current?.emit('draw', { roomId, segment });
    prevPosRef.current = pos;
  };

  const handleMouseUp = () => {
    isDrawingRef.current = false;
    prevPosRef.current = null;
  };

  const handleClear = () => {
    const canvas = canvasRef.current;
    ctxRef.current.clearRect(0, 0, canvas.width, canvas.height);
    socketRef.current?.emit('clear', roomId);
  };

  const copyLink = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="whiteboard-layout">
      <header className="app-header">
        <div className="header-info">
          <span className="app-title">Whiteboard</span>
          <span className="room-badge">Room: {roomId}</span>
          <span className="online-indicator">{userCount} online</span>
        </div>
        <button className="share-btn" onClick={copyLink}>
          {copied ? 'Link Copied!' : 'Share Room'}
        </button>
      </header>

      <div className="workspace">
        <Toolbar
          tool={tool}
          color={color}
          lineWidth={lineWidth}
          onToolChange={setTool}
          onColorChange={setColor}
          onLineWidthChange={setLineWidth}
          onClear={handleClear}
        />

        <div className="canvas-area">
          <canvas
            ref={canvasRef}
            className="canvas"
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
            onTouchStart={handleMouseDown}
            onTouchMove={handleMouseMove}
            onTouchEnd={handleMouseUp}
            style={{ cursor: tool === 'eraser' ? 'cell' : 'crosshair' }}
          />
          {Object.entries(cursors).map(([id, pos]) => (
            <div
              key={id}
              className="remote-cursor"
              style={{ left: pos.x, top: pos.y }}
            />
          ))}
        </div>
      </div>
    </div>
  );
}
