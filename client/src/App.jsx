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
  const [activeRoom, setActiveRoom] = useState('');

  useEffect(() => {
    const url = new URL(window.location.href);
    const existingRoom = resolveRoomInput(url.searchParams.get('room') ?? '');
    const roomId = existingRoom || randomRoomId();

    if (!existingRoom) {
      url.searchParams.set('room', roomId);
      window.history.replaceState({}, '', url);
    }

    setActiveRoom(roomId);
  }, []);

  const enterRoom = (roomId) => {
    const nextRoomId = resolveRoomInput(roomId);
    if (!nextRoomId) return false;

    const url = new URL(window.location.href);
    url.searchParams.set('room', nextRoomId);
    window.history.pushState({}, '', url);
    setActiveRoom(nextRoomId);
    return true;
  };

  if (!activeRoom) return null;

  return <Whiteboard roomId={activeRoom} onJoinRoom={enterRoom} />;
}
