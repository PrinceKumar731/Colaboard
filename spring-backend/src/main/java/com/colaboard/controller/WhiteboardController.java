package com.colaboard.controller;

import com.colaboard.model.CursorMessage;
import com.colaboard.model.DrawSegment;
import com.colaboard.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.StompHeaderAccessor;

import java.util.Map;
import java.util.List;

@Controller
public class WhiteboardController {

    @Autowired
    private SimpMessagingTemplate messaging;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/room/{roomId}/join")
    public void join(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String clientId = resolveClientId(accessor);
        roomService.addSession(roomId, sessionId, clientId);
        RoomService.SessionInfo sessionInfo = roomService.getSessionInfo(sessionId);

        // Send this user's own session ID so the frontend can filter self-cursor
        messaging.convertAndSendToUser(clientId, "/queue/session", clientId);
        messaging.convertAndSendToUser(clientId, "/queue/identity", Map.of(
                "clientId", clientId,
                "displayName", sessionInfo != null ? sessionInfo.getDisplayName() : "User"
        ));

        // Send canvas history only to the joining user
        List<DrawSegment> history = roomService.getHistory(roomId);
        messaging.convertAndSendToUser(clientId, "/queue/canvas-state", history);
        messaging.convertAndSend("/topic/room/" + roomId + "/state", history);

        // Broadcast updated user count to everyone in the room
        messaging.convertAndSend("/topic/room/" + roomId + "/users", roomService.getUserCount(roomId));
    }

    @MessageMapping("/room/{roomId}/draw")
    public void draw(@DestinationVariable String roomId, @Payload DrawSegment segment) {
        roomService.addToHistory(roomId, segment);
        messaging.convertAndSend("/topic/room/" + roomId + "/draw", segment);
        messaging.convertAndSend("/topic/room/" + roomId + "/state", roomService.getHistory(roomId));
    }

    @MessageMapping("/room/{roomId}/cursor")
    public void cursor(@DestinationVariable String roomId,
                       @Payload CursorMessage cursor,
                       SimpMessageHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        RoomService.SessionInfo sessionInfo = sessionId != null ? roomService.getSessionInfo(sessionId) : null;
        cursor.setSessionId(resolveClientId(accessor));
        cursor.setDisplayName(sessionInfo != null ? sessionInfo.getDisplayName() : "User");
        messaging.convertAndSend("/topic/room/" + roomId + "/cursor", cursor);
    }

    @MessageMapping("/room/{roomId}/clear")
    public void clear(@DestinationVariable String roomId) {
        roomService.clearHistory(roomId);
        messaging.convertAndSend("/topic/room/" + roomId + "/clear", "");
        messaging.convertAndSend("/topic/room/" + roomId + "/state", roomService.getHistory(roomId));
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        RoomService.SessionInfo sessionInfo = roomService.removeSession(sessionId);
        if (sessionInfo == null) return;

        messaging.convertAndSend("/topic/room/" + sessionInfo.getRoomId() + "/cursor-leave", sessionInfo.getClientId());
        messaging.convertAndSend("/topic/room/" + sessionInfo.getRoomId() + "/users", roomService.getUserCount(sessionInfo.getRoomId()));
    }

    private String resolveClientId(SimpMessageHeaderAccessor accessor) {
        if (accessor.getUser() != null && accessor.getUser().getName() != null) {
            return accessor.getUser().getName();
        }
        if (accessor.getSessionId() != null) {
            return accessor.getSessionId();
        }
        return "unknown-session";
    }
}
