import { useState, useEffect } from 'react';
import Whiteboard from './components/Whiteboard';

function randomRoomId() {
  return Math.random().toString(36).slice(2, 8).toUpperCase();
}

export default function App() {
  const [activeRoom, setActiveRoom] = useState(null);
  const [joinInput, setJoinInput] = useState('');

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const room = params.get('room');
    if (room) setActiveRoom(room);
  }, []);

  const enterRoom = (id) => {
    const url = new URL(window.location.href);
    url.searchParams.set('room', id);
    window.history.pushState({}, '', url);
    setActiveRoom(id);
  };

  const handleCreate = () => enterRoom(randomRoomId());

  const handleJoin = (e) => {
    e.preventDefault();
    const id = joinInput.trim().toUpperCase();
    if (id) enterRoom(id);
  };

  if (activeRoom) return <Whiteboard roomId={activeRoom} />;

  return (
    <div className="landing">
      <div className="landing-card">
        <div className="landing-icon">&#9634;</div>
        <h1>Collaborative Whiteboard</h1>
        <p>Draw together in real time. Create a room and share the link with anyone.</p>

        <button className="btn-create" onClick={handleCreate}>
          Create New Room
        </button>

        <div className="or-divider">
          <span>or join existing</span>
        </div>

        <form className="join-form" onSubmit={handleJoin}>
          <input
            value={joinInput}
            onChange={(e) => setJoinInput(e.target.value)}
            placeholder="Enter Room ID"
            spellCheck={false}
          />
          <button type="submit">Join</button>
        </form>
      </div>
    </div>
  );
}
