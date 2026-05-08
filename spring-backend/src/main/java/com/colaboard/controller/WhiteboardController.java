package com.colaboard.controller;

import com.colaboard.model.CursorMessage;
import com.colaboard.model.DrawSegment;
import com.colaboard.model.JoinRoomMessage;
import com.colaboard.security.WebSocketUserPrincipal;
import com.colaboard.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.List;
import java.util.Map;

@Controller
public class WhiteboardController {
    private static final Logger logger = LoggerFactory.getLogger(WhiteboardController.class);

    @Autowired
    private RoomService roomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomId}/join")
    public void join(@DestinationVariable String roomId,
                     @Payload(required = false) JoinRoomMessage joinMessage,
                     SimpMessageHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        WebSocketUserPrincipal principal = requirePrincipal(accessor);
        String clientId = joinMessage != null && joinMessage.getClientId() != null && !joinMessage.getClientId().isBlank()
                ? joinMessage.getClientId()
                : sessionId;

        roomService.addSession(roomId, sessionId, clientId, principal.getDisplayName());
        List<DrawSegment> history = roomService.getHistory(roomId);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/session", Map.of(
                "roomId", roomId,
                "sessionId", sessionId,
                "clientId", clientId,
                "displayName", principal.getDisplayName()
        ));
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/canvas-state", Map.of(
                "roomId", roomId,
                "elements", history
        ));

        roomService.publishState(roomId);
        roomService.publishUsers(roomId);
    }

    @MessageMapping("/room/{roomId}/draw")
    public void draw(@DestinationVariable String roomId, @Payload DrawSegment segment) {
        roomService.addToHistory(roomId, segment);
        roomService.publishDraw(roomId, segment);
        roomService.publishState(roomId);
    }

    @MessageMapping("/room/{roomId}/cursor")
    public void cursor(@DestinationVariable String roomId,
                       @Payload CursorMessage cursor,
                       SimpMessageHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        RoomService.SessionInfo sessionInfo = sessionId != null ? roomService.getSessionInfo(sessionId) : null;
        cursor.setSessionId(sessionInfo != null ? sessionInfo.getClientId() : sessionId);
        cursor.setDisplayName(sessionInfo != null ? sessionInfo.getDisplayName() : "User");
        roomService.publishCursor(roomId, cursor);
    }

    @MessageMapping("/room/{roomId}/clear")
    public void clear(@DestinationVariable String roomId) {
        roomService.clearHistory(roomId);
        roomService.publishClear(roomId);
        roomService.publishState(roomId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        RoomService.SessionInfo sessionInfo = roomService.removeSession(sessionId);
        if (sessionInfo == null) return;

        roomService.publishCursorLeave(sessionInfo.getRoomId(), sessionInfo.getClientId());
        roomService.publishUsers(sessionInfo.getRoomId());
    }

    @MessageExceptionHandler
    public void handleMessagingException(Exception exception) {
        logger.error("Whiteboard websocket message failed", exception);
    }

    private WebSocketUserPrincipal requirePrincipal(SimpMessageHeaderAccessor accessor) {
        if (accessor.getUser() instanceof WebSocketUserPrincipal principal) {
            return principal;
        }
        return new WebSocketUserPrincipal(
                accessor.getSessionId(),
                "anon-" + accessor.getSessionId(),
                "guest@local",
                "Guest",
                "ANONYMOUS"
        );
    }
}
