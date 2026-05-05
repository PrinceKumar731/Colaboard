package com.colaboard.service;

import com.colaboard.model.DrawSegment;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RoomService {

    // roomId -> list of draw segments
    private final Map<String, List<DrawSegment>> roomHistory = new ConcurrentHashMap<>();

    // websocket sessionId -> session info
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    // roomId -> set of websocket sessionIds
    private final Map<String, Set<String>> roomSessions = new ConcurrentHashMap<>();

    public void addSession(String roomId, String sessionId, String clientId) {
        sessions.put(sessionId, new SessionInfo(roomId, clientId));
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        roomHistory.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
    }

    public SessionInfo removeSession(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            Set<String> activeSessions = roomSessions.get(info.getRoomId());
            if (activeSessions != null) {
                activeSessions.remove(sessionId);
                if (activeSessions.isEmpty()) {
                    roomSessions.remove(info.getRoomId());
                }
            }
        }
        return info;
    }

    public SessionInfo getSessionInfo(String sessionId) {
        return sessions.get(sessionId);
    }

    public List<DrawSegment> getHistory(String roomId) {
        return List.copyOf(roomHistory.getOrDefault(roomId, Collections.emptyList()));
    }

    public void addToHistory(String roomId, DrawSegment segment) {
        roomHistory.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(segment);
    }

    public void clearHistory(String roomId) {
        List<DrawSegment> history = roomHistory.get(roomId);
        if (history != null) history.clear();
    }

    public int getUserCount(String roomId) {
        Set<String> activeSessions = roomSessions.get(roomId);
        return activeSessions != null ? activeSessions.size() : 0;
    }

    public static class SessionInfo {
        private final String roomId;
        private final String clientId;

        public SessionInfo(String roomId, String clientId) {
            this.roomId = roomId;
            this.clientId = clientId;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getClientId() {
            return clientId;
        }
    }
}
