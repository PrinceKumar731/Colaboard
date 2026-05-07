import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/ws';
const STOMP_HOST = __ENV.STOMP_HOST || parseHost(WS_URL);
const ROOM_ID = __ENV.ROOM_ID || 'LOADTEST';
const USERS = Number(__ENV.USERS || 100);
const MODE = (__ENV.MODE || 'cursor').toLowerCase();
const RAMP_UP = __ENV.RAMP_UP || '20s';
const HOLD_FOR = __ENV.HOLD_FOR || '1m';
const RAMP_DOWN = __ENV.RAMP_DOWN || '10s';
const SESSION_SECONDS = Number(__ENV.SESSION_SECONDS || 60);
const CURSOR_INTERVAL_MS = Number(__ENV.CURSOR_INTERVAL_MS || (MODE === 'cursor-storm' ? 25 : 1000));
const DRAW_MIN_MS = Number(__ENV.DRAW_MIN_MS || 20);
const DRAW_MAX_MS = Number(__ENV.DRAW_MAX_MS || 50);
const SEND_DRAW = MODE === 'draw' || (__ENV.SEND_DRAW || 'false').toLowerCase() === 'true';
const SEND_CURSOR = MODE !== 'draw-only';

const connectedUsers = new Counter('whiteboard_connected_users');
const joinedUsers = new Counter('whiteboard_joined_users');
const stompErrors = new Counter('whiteboard_stomp_errors');
const cursorMessages = new Counter('whiteboard_cursor_messages');
const drawMessages = new Counter('whiteboard_draw_messages');
const websocketFailures = new Rate('whiteboard_websocket_failures');
const connectTime = new Trend('whiteboard_connect_time_ms');

function parseHost(url) {
  return url.replace(/^wss?:\/\//, '').split('/')[0];
}

export const options = {
  scenarios: {
    join_single_room: {
      executor: 'ramping-vus',
      stages: [
        { duration: RAMP_UP, target: USERS },
        { duration: HOLD_FOR, target: USERS },
        { duration: RAMP_DOWN, target: 0 },
      ],
    },
  },
  thresholds: {
    whiteboard_websocket_failures: ['rate<0.01'],
    whiteboard_connected_users: [`count>=${USERS}`],
    whiteboard_joined_users: [`count>=${USERS}`],
  },
};

function stompFrame(command, headers = {}, body = '') {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
  return `${command}\n${headerLines.join('\n')}\n\n${body}\x00`;
}

function sendJoin(socket, clientId) {
  socket.send(stompFrame(
    'SEND',
    {
      destination: `/app/room/${ROOM_ID}/join`,
      'content-type': 'application/json',
    },
    JSON.stringify({ clientId }),
  ));
  joinedUsers.add(1);
}

function sendCursor(socket, vu, iteration) {
  socket.send(stompFrame(
    'SEND',
    {
      destination: `/app/room/${ROOM_ID}/cursor`,
      'content-type': 'application/json',
    },
    JSON.stringify({
      x: 100 + ((vu * 17 + iteration * 13) % 1200),
      y: 100 + ((vu * 23 + iteration * 11) % 700),
    }),
  ));
  cursorMessages.add(1);
}

function sendDraw(socket, clientId, vu, iteration) {
  const now = Date.now().toString(36);
  const x = 80 + ((vu * 31 + iteration * 17) % 1300);
  const y = 80 + ((vu * 19 + iteration * 23) % 760);
  socket.send(stompFrame(
    'SEND',
    {
      destination: `/app/room/${ROOM_ID}/draw`,
      'content-type': 'application/json',
    },
    JSON.stringify({
      id: `k6-${clientId}-${now}`,
      elementType: 'stroke',
      tool: 'pen',
      color: '#1f2937',
      lineWidth: 2,
      startX: x,
      startY: y,
      endX: x + 120,
      endY: y + 80,
      points: [x, y, x + 30, y + 20, x + 70, y + 48, x + 120, y + 80],
    }),
  ));
  drawMessages.add(1);
}

function randomDrawIntervalMs(vu, iteration) {
  const spread = Math.max(0, DRAW_MAX_MS - DRAW_MIN_MS);
  return DRAW_MIN_MS + ((vu * 37 + iteration * 17) % (spread + 1));
}

export default function () {
  const clientId = `k6-vu-${__VU}-${__ITER}-${Date.now()}`;
  const startedAt = Date.now();

  const response = ws.connect(WS_URL, {}, (socket) => {
    let connected = false;
    let cursorIteration = 0;

    socket.on('open', () => {
      socket.send(stompFrame('CONNECT', {
        'accept-version': '1.2',
        host: STOMP_HOST,
        'heart-beat': '0,0',
      }));
    });

    socket.on('message', (message) => {
      if (message.startsWith('CONNECTED')) {
        connected = true;
        connectedUsers.add(1);
        connectTime.add(Date.now() - startedAt);

        socket.send(stompFrame('SUBSCRIBE', {
          id: `state-${clientId}`,
          destination: `/topic/room/${ROOM_ID}/state`,
        }));

        socket.send(stompFrame('SUBSCRIBE', {
          id: `users-${clientId}`,
          destination: `/topic/room/${ROOM_ID}/users`,
        }));

        sendJoin(socket, clientId);

        if (SEND_CURSOR) {
          socket.setInterval(() => {
            cursorIteration += 1;
            sendCursor(socket, __VU, cursorIteration);
          }, CURSOR_INTERVAL_MS);
        }

        if (SEND_DRAW) {
          const drawLoop = () => {
            cursorIteration += 1;
            sendDraw(socket, clientId, __VU, cursorIteration);
            socket.setTimeout(drawLoop, randomDrawIntervalMs(__VU, cursorIteration));
          };

          socket.setTimeout(drawLoop, randomDrawIntervalMs(__VU, cursorIteration));
        }

        socket.setTimeout(() => {
          socket.send(stompFrame('DISCONNECT', { receipt: `bye-${clientId}` }));
          socket.close();
        }, SESSION_SECONDS * 1000);
      }

      if (message.startsWith('ERROR')) {
        stompErrors.add(1);
        console.error(`STOMP error for ${clientId}: ${message}`);
        socket.close();
      }
    });

    socket.on('error', (error) => {
      websocketFailures.add(1);
      console.error(`WebSocket error for ${clientId}: ${error.error()}`);
    });

    socket.on('close', () => {
      check(connected, {
        'STOMP connected': (value) => value === true,
      });
    });
  });

  check(response, {
    'WebSocket upgrade status is 101': (result) => result && result.status === 101,
  });

  sleep(1);
}
