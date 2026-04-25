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

import java.util.List;

@Controller
public class WhiteboardController {

    @Autowired
    private SimpMessagingTemplate messaging;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/room/{roomId}/join")
    public void join(@DestinationVariable String roomId, SimpMessageHeaderAccessor accessor) {
        String principal = accessor.getUser().getName();
        roomService.addSession(roomId, principal);

        // Send this user's own session ID so the frontend can filter self-cursor
        messaging.convertAndSendToUser(principal, "/queue/session", principal);

        // Send canvas history only to the joining user
        List<DrawSegment> history = roomService.getHistory(roomId);
        messaging.convertAndSendToUser(principal, "/queue/canvas-state", history);

        // Broadcast updated user count to everyone in the room
        messaging.convertAndSend("/topic/room/" + roomId + "/users", roomService.getUserCount(roomId));
    }

    @MessageMapping("/room/{roomId}/draw")
    public void draw(@DestinationVariable String roomId, @Payload DrawSegment segment) {
        roomService.addToHistory(roomId, segment);
        messaging.convertAndSend("/topic/room/" + roomId + "/draw", segment);
    }

    @MessageMapping("/room/{roomId}/cursor")
    public void cursor(@DestinationVariable String roomId,
                       @Payload CursorMessage cursor,
                       SimpMessageHeaderAccessor accessor) {
        cursor.setSessionId(accessor.getUser().getName());
        messaging.convertAndSend("/topic/room/" + roomId + "/cursor", cursor);
    }

    @MessageMapping("/room/{roomId}/clear")
    public void clear(@DestinationVariable String roomId) {
        roomService.clearHistory(roomId);
        messaging.convertAndSend("/topic/room/" + roomId + "/clear", "");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        if (event.getUser() == null) return;
        String principal = event.getUser().getName();
        String roomId = roomService.removeSession(principal);
        if (roomId == null) return;

        messaging.convertAndSend("/topic/room/" + roomId + "/cursor-leave", principal);
        messaging.convertAndSend("/topic/room/" + roomId + "/users", roomService.getUserCount(roomId));
    }
}
