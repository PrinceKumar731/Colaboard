# Collaborative Whiteboard Project Context

Last updated: 2026-05-07

## Project Goal

This repo is primarily a backend-focused collaborative whiteboard. The frontend exists to exercise the core real-time drawing features, but the main value is the Spring backend, websocket collaboration, Redis-backed shared state, room handling, and eventual authentication.

The whiteboard is intended to feel similar to Excalidraw: large grid canvas, floating controls, drawing tools, shape tools, room sharing, and live multi-user collaboration.

## Active App

Use these folders for the active whiteboard project:

- Frontend: `client`
- Backend: `spring-backend`
- Redis compose file: `docker-compose.redis.yml`

There is also an older Node/socket.io server in `server`. It is not the active backend for the current Spring/Vite app.

There is also a separate `code-judge` project in the repo. It is unrelated to the whiteboard.

## Current Run Flow

Redis is required because room history, presence, and Redis pub/sub are used by the Spring backend.

Start Redis:

```powershell
npm run redis:up
```

Then start Spring + Vite:

```powershell
npm run dev
```

Frontend URL:

```text
http://localhost:5173
```

Spring backend:

```text
http://localhost:8080
```

If Docker is not installed, Redis must be installed or started another way on:

```text
localhost:6379
```

## Root Scripts

From `package.json`:

- `npm run dev`: starts Spring backend and Vite client together.
- `npm run redis:up`: starts Redis using Docker Compose.
- `npm run redis:down`: stops Redis.
- `npm run spring`: starts Spring backend only.
- `npm run build`: builds the Vite client.
- `npm run build:spring`: packages the Spring backend with tests skipped.
- `npm run dev:node`: old Node/socket.io flow, not currently recommended.

## Environment Variables

The project may have a root `.env`, but Spring does not automatically load root `.env` by itself. Vite loads `VITE_*` vars from `.env`; Spring reads real process environment variables or values from `application.properties`.

Current Spring properties in `spring-backend/src/main/resources/application.properties`:

```properties
server.port=8080
spring.application.name=colaboard-backend
spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
app.jwt.secret=${JWT_SECRET:change-this-super-secret-key-change-this-super-secret-key}
app.jwt.expiration-minutes=${JWT_EXPIRATION_MINUTES:120}
app.google.client-id=${GOOGLE_CLIENT_ID:}
```

Useful variables:

```env
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=change-this-to-a-long-random-secret-at-least-32-chars
JWT_EXPIRATION_MINUTES=120
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
```

Important: authentication is currently bypassed in the active flow, so `JWT_SECRET`, `GOOGLE_CLIENT_ID`, and `VITE_GOOGLE_CLIENT_ID` are not required to use the whiteboard right now.

## Frontend Behavior

Main files:

- `client/src/App.jsx`
- `client/src/components/Whiteboard.jsx`
- `client/src/components/Toolbar.jsx`
- `client/src/utils/canvas.js`
- `client/src/App.css`

Current frontend state:

- No landing page.
- Opening the site immediately creates or joins a room.
- Room id is stored in the URL query parameter: `?room=ABC123`.
- The `Join` button opens a modal accepting either a room code or a full invite URL.
- The `Share` button opens a modal with the current room link and copy button.
- The canvas fills the viewport with floating controls.
- The toolbar is compact and icon-based.
- Supports pen, line, arrow, rectangle, ellipse, diamond, eraser, clear, stroke color, fill option, stroke width slider, zoom, pan, and PNG download.
- Remote cursors show labels for other connected users.

Zoom/pan controls:

- Mouse wheel pans.
- Ctrl + mouse wheel zooms.
- Space + drag pans.
- `+`, `-`, and `Reset` controls are visible on the canvas.

## Backend Architecture

Main files:

- `spring-backend/src/main/java/com/colaboard/controller/WhiteboardController.java`
- `spring-backend/src/main/java/com/colaboard/service/RoomService.java`
- `spring-backend/src/main/java/com/colaboard/service/RedisRoomEventSubscriber.java`
- `spring-backend/src/main/java/com/colaboard/config/RedisConfig.java`
- `spring-backend/src/main/java/com/colaboard/config/WebSocketConfig.java`
- `spring-backend/src/main/java/com/colaboard/config/WebSocketAuthChannelInterceptor.java`

Websocket endpoint:

```text
/ws
```

Client sends STOMP messages to:

- `/app/room/{roomId}/join`
- `/app/room/{roomId}/draw`
- `/app/room/{roomId}/cursor`
- `/app/room/{roomId}/clear`

Client subscribes to:

- `/user/queue/session`
- `/user/queue/canvas-state`
- `/topic/room/{roomId}/state`
- `/topic/room/{roomId}/draw`
- `/topic/room/{roomId}/cursor`
- `/topic/room/{roomId}/cursor-leave`
- `/topic/room/{roomId}/users`
- `/topic/room/{roomId}/clear`

Redis keys used by `RoomService`:

- `colaboard:room:{roomId}:history`
- `colaboard:room:{roomId}:participants`

Redis pub/sub topic:

- `colaboard:room-events`

Redis event types:

- `state`
- `draw`
- `cursor`
- `clear`
- `users`
- `cursor_leave`

## Collaboration Model

The backend stores full drawing history in Redis. When a user joins, they receive history from Redis and the room also publishes a full state event.

For live drawing:

1. Client draws locally for immediate feedback.
2. Client publishes a draw element to Spring over STOMP.
3. Spring stores the element in Redis.
4. Spring publishes Redis events.
5. All Spring instances receive the Redis event and broadcast it to their local websocket clients.

This makes the app work better when multiple users or multiple backend instances are involved.

## Drawing Data Model

Drawing payload class:

- `spring-backend/src/main/java/com/colaboard/model/DrawSegment.java`

Despite the name `DrawSegment`, it now represents both freehand strokes and shape elements.

Important fields:

- `id`
- `elementType`
- `tool`
- `color`
- `fillColor`
- `lineWidth`
- `points`
- `startX`
- `startY`
- `endX`
- `endY`

Supported tools:

- `pen`
- `eraser`
- `line`
- `arrow`
- `rectangle`
- `ellipse`
- `diamond`

## Authentication Status

Authentication code exists but is currently bypassed for the active whiteboard flow.

Implemented but not active as a required gate:

- `AuthController`
- `UserAccountService`
- `JwtService`
- `GoogleIdentityService`
- `SecurityConfig`
- `JwtAuthenticationFilter`
- `AuthenticatedUser`
- `WebSocketUserPrincipal`
- DTOs under `spring-backend/src/main/java/com/colaboard/dto`

Current temporary behavior:

- The frontend opens the whiteboard directly.
- The frontend does not send JWT headers on websocket connect.
- `WebSocketAuthChannelInterceptor` creates an anonymous guest identity when no JWT is present.
- `WhiteboardController` also has a guest fallback.

This was done intentionally to keep collaboration working while auth is paused.

To re-enable auth later:

1. Restore the auth gate in `client/src/App.jsx`.
2. Pass `authToken`, `authUser`, and `onLogout` into `Whiteboard`.
3. Re-add `connectHeaders.Authorization = Bearer {token}` in `Whiteboard.jsx`.
4. Make `WebSocketAuthChannelInterceptor` reject missing JWT instead of creating anonymous users.
5. Decide whether REST endpoints beyond `/api/auth/**` should be protected.

## Known Operational Requirements

Redis must be running before Spring starts. If Redis is down, Spring fails with:

```text
Failed to start bean 'redisMessageListenerContainer'
Unable to connect to Redis
Connection refused: localhost/127.0.0.1:6379
```

If Spring is down, Vite proxy requests fail with:

```text
http proxy error: /api/...
ECONNREFUSED
```

That is a backend availability symptom, not usually a Vite bug.

## Recent Verification

These commands passed after the latest auth bypass change:

```powershell
npm run build
mvn -q -DskipTests compile
```

Maven may require normal user access to the local Maven repository. In the Codex sandbox, Maven sometimes needs elevated execution because the sandbox user cannot write to the default `.m2` directory.

## Design Notes

The UI should stay closer to a tool than a marketing page:

- No landing page.
- The drawing canvas is the first screen.
- Controls float above the grid.
- Keep descriptive text out of the whiteboard surface.
- Prefer compact icon controls and small modals.
- Avoid large decorative panels that reduce drawing space.

## Good Next Tasks

High-value backend tasks:

- Add integration tests for join, draw, clear, and Redis state replay.
- Add Docker Compose for backend + Redis together.
- Add graceful Redis health check / clearer startup error.
- Add room metadata and optional room ownership.
- Add rate limits for cursor events and draw events.

High-value frontend tasks:

- Add undo/redo.
- Add selection, move, resize.
- Add text tool.
- Add fit-to-content.
- Add better mobile toolbar.

Auth tasks when ready:

- Re-enable JWT auth gate.
- Add refresh tokens.
- Add logout invalidation.
- Add password reset.
- Add Google OAuth production config instructions.
