const express = require('express');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: { origin: '*' }
});

const roomHistory = new Map();

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

io.on('connection', (socket) => {
  socket.on('join-room', (roomId) => {
    socket.join(roomId);
    socket.data.roomId = roomId;

    socket.emit('canvas-state', roomHistory.get(roomId) || []);

    const count = io.sockets.adapter.rooms.get(roomId)?.size ?? 0;
    io.to(roomId).emit('room-users', count);
  });

  socket.on('draw', ({ roomId, segment }) => {
    if (!roomHistory.has(roomId)) roomHistory.set(roomId, []);
    roomHistory.get(roomId).push(segment);
    socket.to(roomId).emit('draw', segment);
  });

  socket.on('cursor', ({ roomId, x, y }) => {
    socket.to(roomId).emit('cursor', { id: socket.id, x, y });
  });

  socket.on('clear', (roomId) => {
    roomHistory.set(roomId, []);
    io.to(roomId).emit('clear');
  });

  socket.on('disconnect', () => {
    const roomId = socket.data.roomId;
    if (!roomId) return;
    const count = io.sockets.adapter.rooms.get(roomId)?.size ?? 0;
    io.to(roomId).emit('room-users', count);
    io.to(roomId).emit('cursor-leave', socket.id);
  });
});

const PORT = process.env.PORT || 3001;
server.listen(PORT, () => console.log(`Server listening on :${PORT}`));
