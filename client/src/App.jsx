import { useEffect, useState } from 'react';
import Whiteboard from './components/Whiteboard';

function randomRoomId() {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

function resolveRoomInput(value) {
  const trimmed = value.trim();
  if (!trimmed) return '';

  try {
    const parsed = new URL(trimmed);
    return parsed.searchParams.get('room')?.trim().toUpperCase() ?? '';
  } catch {
    return trimmed.toUpperCase();
  }
}

export default function App() {
  const [activeRoom, setActiveRoom] = useState(null);
  const [joinInput, setJoinInput] = useState('');

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const room = params.get('room');
    if (room) setActiveRoom(resolveRoomInput(room));
  }, []);

  const enterRoom = (roomId) => {
    const nextRoomId = resolveRoomInput(roomId);
    if (!nextRoomId) return;
    const url = new URL(window.location.href);
    url.searchParams.set('room', nextRoomId);
    window.history.pushState({}, '', url);
    setActiveRoom(nextRoomId);
  };

  const handleCreate = () => enterRoom(randomRoomId());

  const handleJoin = (event) => {
    event.preventDefault();
    const roomId = resolveRoomInput(joinInput);
    if (roomId) enterRoom(roomId);
  };

  if (activeRoom) {
    return <Whiteboard roomId={activeRoom} />;
  }

  return (
    <div className="landing-shell">
      <div className="landing-noise" />
      <section className="hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">Collaborative whiteboard</span>
          <h1>Turn this board into your team&apos;s live sketch room.</h1>
          <p>
            Fast room sharing, Excalidraw-inspired tools, and real-time collaboration on a
            spacious canvas made for rough ideas.
          </p>

          <div className="hero-actions">
            <button type="button" className="primary-cta" onClick={handleCreate}>
              Create room
            </button>
            <form className="join-room-form" onSubmit={handleJoin}>
              <input
                value={joinInput}
                onChange={(event) => setJoinInput(event.target.value)}
                placeholder="Enter room code"
                spellCheck={false}
              />
              <button type="submit">Join</button>
            </form>
          </div>
        </div>

        <div className="hero-preview">
          <div className="preview-card sticky">
            <span className="preview-label">Feels like</span>
            <strong>Loose, visual, and collaborative</strong>
            <p>
              Pencil, arrows, rectangles, ellipses, diamonds, eraser, export, and room-level
              sync all in one board.
            </p>
          </div>
          <div className="preview-card">
            <span className="preview-label">Best for</span>
            <strong>Flows, architecture, notes, quick wireframes</strong>
            <p>Share one link, jump into a room, and sketch together immediately.</p>
          </div>
        </div>
      </section>
    </div>
  );
}
