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

    // principalName -> roomId  (for disconnect cleanup)
    private final Map<String, String> principalRoom = new ConcurrentHashMap<>();

    // roomId -> set of principalNames
    private final Map<String, Set<String>> roomPrincipals = new ConcurrentHashMap<>();

    public void addSession(String roomId, String principalName) {
        principalRoom.put(principalName, roomId);
        roomPrincipals.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(principalName);
        roomHistory.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
    }

    public String removeSession(String principalName) {
        String roomId = principalRoom.remove(principalName);
        if (roomId != null) {
            Set<String> principals = roomPrincipals.get(roomId);
            if (principals != null) principals.remove(principalName);
        }
        return roomId;
    }

    public String getRoomForPrincipal(String principalName) {
        return principalRoom.get(principalName);
    }

    public List<DrawSegment> getHistory(String roomId) {
        return roomHistory.getOrDefault(roomId, Collections.emptyList());
    }

    public void addToHistory(String roomId, DrawSegment segment) {
        roomHistory.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(segment);
    }

    public void clearHistory(String roomId) {
        List<DrawSegment> history = roomHistory.get(roomId);
        if (history != null) history.clear();
    }

    public int getUserCount(String roomId) {
        Set<String> principals = roomPrincipals.get(roomId);
        return principals != null ? principals.size() : 0;
    }
}
