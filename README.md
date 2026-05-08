# Colaboard

Colaboard is a collaborative whiteboard application inspired by Excalidraw. It provides a full-screen drawing surface, live multi-user collaboration over WebSockets, room-based sharing, and Redis-backed whiteboard state replay.

This repository contains multiple projects, but the active whiteboard stack is:

- Frontend: `client`
- Backend: `spring-backend`
- Redis compose file: `docker-compose.redis.yml`

The `server` directory contains an older Node.js Socket.IO implementation and is not the active backend for the current app. The `code-judge` directory is a separate project and is unrelated to the whiteboard.

## Features

- Real-time collaboration with room-based sessions
- Freehand drawing and shape tools
- Shareable room links
- Remote cursors and participant count
- Zoom, pan, clear board, and PNG export
- Redis-backed room history and pub/sub fanout
- Spring Boot STOMP WebSocket backend

## Tech Stack

- React 18
- Vite
- Spring Boot 3
- STOMP over WebSocket
- Redis
- Maven

## Repository Structure

```text
.
├── client/                React frontend
├── spring-backend/        Spring Boot backend
├── server/                Legacy Node/socket.io backend
├── code-judge/            Separate unrelated project
├── docker-compose.redis.yml
├── package.json
└── PROJECT_CONTEXT.md
```

## Local Development

### Prerequisites

- Node.js 18 or later
- Java 17
- Maven
- Redis

If Docker Desktop is available, Redis can be started through Docker Compose. Otherwise, run Redis manually on `localhost:6379`.

### Install Dependencies

```powershell
npm install
npm install --prefix client
```

### Start Redis

```powershell
npm run redis:up
```

If Docker is not available, start Redis locally by another method before launching the backend.

### Start Frontend and Backend

```powershell
npm run dev
```

This starts:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

### Useful Commands

```powershell
npm run spring
npm run build
npm run build:spring
npm run redis:down
```

## Environment Variables

### Frontend

Frontend variables are read by Vite from `client/.env` or deployment environment settings.

Local development:

```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=
```

Notes:

- `VITE_WS_URL` can be omitted locally. The frontend can use the Vite dev proxy for `/ws`.
- In production, `VITE_WS_URL` should be the real backend WebSocket URL.

Production example:

```env
VITE_API_URL=https://your-backend-domain.com
VITE_WS_URL=wss://your-backend-domain.com/ws
```

### Backend

Spring reads environment variables from the host or deployment platform.

Common backend variables:

```env
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL=true
JWT_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION_MINUTES=120
GOOGLE_CLIENT_ID=
FRONTEND_URL=http://localhost:5173
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

Important notes:

- `FRONTEND_URL` must be set correctly in production. It is used for CORS and WebSocket origin handling.
- If your frontend is hosted on Vercel and backend on Render, `FRONTEND_URL` should be your Vercel app URL.
- Hosted Redis providers often require `REDIS_SSL=true`.

## Deployment

### Vercel Frontend + Render Backend

Vercel frontend environment:

```env
VITE_API_URL=https://your-render-backend.onrender.com
VITE_WS_URL=wss://your-render-backend.onrender.com/ws
```

Render backend environment:

```env
FRONTEND_URL=https://your-vercel-app.vercel.app
REDIS_HOST=...
REDIS_PORT=...
REDIS_PASSWORD=...
REDIS_SSL=true
JWT_SECRET=...
```

Deployment checklist:

1. Set the Vercel frontend variables to the Render backend URL.
2. Set `FRONTEND_URL` on Render to the Vercel frontend URL.
3. Configure Redis connection variables on Render.
4. Redeploy both services after changing environment variables.

## Collaboration Architecture

The collaboration flow uses STOMP over WebSocket with Redis-backed state sharing.

### WebSocket Endpoint

- `/ws`

### Client Publish Destinations

- `/app/room/{roomId}/join`
- `/app/room/{roomId}/draw`
- `/app/room/{roomId}/cursor`
- `/app/room/{roomId}/clear`

### Client Subscriptions

- `/user/queue/session`
- `/user/queue/canvas-state`
- `/topic/room/{roomId}/state`
- `/topic/room/{roomId}/draw`
- `/topic/room/{roomId}/cursor`
- `/topic/room/{roomId}/cursor-leave`
- `/topic/room/{roomId}/users`
- `/topic/room/{roomId}/clear`

### Redis Usage

Redis stores room history and participant presence, and is also used for pub/sub event distribution across backend instances.

Keys:

- `colaboard:room:{roomId}:history`
- `colaboard:room:{roomId}:participants`

Topic:

- `colaboard:room-events`

## Current Product Behavior

- Opening the app creates or resumes a room immediately
- Room IDs are stored in the `room` query parameter
- Users can join by room code or full invite link
- Drawing tools include pen, line, arrow, rectangle, ellipse, diamond, and eraser
- Mouse wheel pans the canvas
- `Ctrl` plus wheel zooms
- `Space` plus drag pans
- The canvas supports export to PNG

## Authentication Status

Authentication code exists in the backend, but the active whiteboard flow currently allows anonymous guest collaboration so the whiteboard can be used without signing in.

Relevant backend components include:

- `AuthController`
- `JwtService`
- `UserAccountService`
- `GoogleIdentityService`
- `SecurityConfig`
- `WebSocketAuthChannelInterceptor`

## Troubleshooting

### WebSocket Connection Fails

Check the following:

1. Confirm the backend is reachable at `http://localhost:8080/health` locally or at your deployed backend URL in production.
2. Confirm Redis is running before starting Spring.
3. Confirm `VITE_API_URL` and `VITE_WS_URL` do not point to `localhost` in production.
4. Confirm `FRONTEND_URL` on the backend matches the deployed frontend origin.
5. Confirm your hosting platform supports WebSocket upgrades on `/ws`.

### Redis Startup Fails

If `npm run redis:up` fails with a Docker daemon error, Docker Desktop is not running. Start Docker Desktop first or run Redis locally without Docker.

### Spring Does Not Start

The backend depends on Redis connectivity at startup. If Redis is unavailable, the application may fail before the WebSocket endpoint becomes available.

## Verification

The following commands are useful for quick verification:

```powershell
npm run build
cd spring-backend
mvn -q -DskipTests compile
```

## Next Improvements

- Undo and redo
- Selection and resizing
- Text tool
- Improved mobile toolbar
- Room metadata and ownership
- Integration tests for whiteboard collaboration flows
