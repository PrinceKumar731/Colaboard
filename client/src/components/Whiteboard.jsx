import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import Toolbar from './Toolbar';
import { drawSegment, replayHistory } from '../utils/canvas';

export default function Whiteboard({ roomId }) {
  const canvasRef = useRef(null);
  const stompRef = useRef(null);
  const isDrawingRef = useRef(false);
  const prevPosRef = useRef(null);
  const ctxRef = useRef(null);
  const mySessionRef = useRef(null);

  const [tool, setTool] = useState('pen');
  const [color, setColor] = useState('#000000');
  const [lineWidth, setLineWidth] = useState(4);
  const [userCount, setUserCount] = useState(1);
  const [cursors, setCursors] = useState({});
  const [copied, setCopied] = useState(false);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = canvas.parentElement;
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;
    ctxRef.current = canvas.getContext('2d');
  }, []);

  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 3000,

      onConnect: () => {
        setConnected(true);

        // Receive own session ID (for filtering self-cursor)
        client.subscribe('/user/queue/session', (msg) => {
          mySessionRef.current = msg.body;
        });

        // Receive canvas history on join
        client.subscribe('/user/queue/canvas-state', (msg) => {
          const history = JSON.parse(msg.body);
          if (ctxRef.current) replayHistory(ctxRef.current, history);
        });

        client.subscribe(`/topic/room/${roomId}/draw`, (msg) => {
          const segment = JSON.parse(msg.body);
          if (ctxRef.current) drawSegment(ctxRef.current, segment);
        });

        client.subscribe(`/topic/room/${roomId}/cursor`, (msg) => {
          const { sessionId, x, y } = JSON.parse(msg.body);
          if (sessionId === mySessionRef.current) return;
          setCursors((prev) => ({ ...prev, [sessionId]: { x, y } }));
        });

        client.subscribe(`/topic/room/${roomId}/cursor-leave`, (msg) => {
          const sessionId = msg.body;
          setCursors((prev) => {
            const next = { ...prev };
            delete next[sessionId];
            return next;
          });
        });

        client.subscribe(`/topic/room/${roomId}/users`, (msg) => {
          setUserCount(Number(msg.body));
        });

        client.subscribe(`/topic/room/${roomId}/clear`, () => {
          const canvas = canvasRef.current;
          if (ctxRef.current) ctxRef.current.clearRect(0, 0, canvas.width, canvas.height);
        });

        // Join the room after all subscriptions are set up
        client.publish({ destination: `/app/room/${roomId}/join`, body: '' });
      },

      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error('STOMP error', frame),
    });

    client.activate();
    stompRef.current = client;

    return () => client.deactivate();
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

    if (stompRef.current?.connected) {
      stompRef.current.publish({
        destination: `/app/room/${roomId}/cursor`,
        body: JSON.stringify({ x: pos.x, y: pos.y }),
      });
    }

    if (!isDrawingRef.current || !prevPosRef.current) return;

    const segment = {
      prevX: prevPosRef.current.x,
      prevY: prevPosRef.current.y,
      x: pos.x,
      y: pos.y,
      color,
      lineWidth,
      tool,
    };

    drawSegment(ctxRef.current, segment);

    if (stompRef.current?.connected) {
      stompRef.current.publish({
        destination: `/app/room/${roomId}/draw`,
        body: JSON.stringify(segment),
      });
    }

    prevPosRef.current = pos;
  };

  const handleMouseUp = () => {
    isDrawingRef.current = false;
    prevPosRef.current = null;
  };

  const handleClear = () => {
    const canvas = canvasRef.current;
    ctxRef.current.clearRect(0, 0, canvas.width, canvas.height);
    if (stompRef.current?.connected) {
      stompRef.current.publish({ destination: `/app/room/${roomId}/clear`, body: '' });
    }
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
          {!connected && <span style={{ fontSize: 11, color: '#ef4444' }}>Connecting…</span>}
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
