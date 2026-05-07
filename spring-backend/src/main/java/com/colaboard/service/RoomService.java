package com.colaboard.service;

import com.colaboard.model.CursorMessage;
import com.colaboard.model.DrawSegment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private static final Logger logger = LoggerFactory.getLogger(RoomService.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChannelTopic roomEventsTopic;
    private final SimpMessagingTemplate messagingTemplate;

    // websocket sessionId -> local session info
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<DrawSegment>> localHistory = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> localParticipants = new ConcurrentHashMap<>();

    public RoomService(StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       ChannelTopic roomEventsTopic,
                       SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.roomEventsTopic = roomEventsTopic;
        this.messagingTemplate = messagingTemplate;
    }

    public void addSession(String roomId, String sessionId, String clientId, String displayName) {
        sessions.put(sessionId, new SessionInfo(roomId, clientId, displayName));
        localParticipants.computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
        try {
            redisTemplate.opsForSet().add(roomParticipantsKey(roomId), sessionId);
        } catch (DataAccessException exception) {
            logger.warn("Redis unavailable while adding participant. Using local room participant state.", exception);
        }
    }

    public SessionInfo removeSession(String sessionId) {
        SessionInfo info = sessions.remove(sessionId);
        if (info != null) {
            Set<String> participants = localParticipants.get(info.getRoomId());
            if (participants != null) {
                participants.remove(sessionId);
            }
            try {
                redisTemplate.opsForSet().remove(roomParticipantsKey(info.getRoomId()), sessionId);
            } catch (DataAccessException exception) {
                logger.warn("Redis unavailable while removing participant. Using local room participant state.", exception);
            }
        }
        return info;
    }

    public SessionInfo getSessionInfo(String sessionId) {
        return sessions.get(sessionId);
    }

    public List<DrawSegment> getHistory(String roomId) {
        List<String> rawSegments;
        try {
            rawSegments = redisTemplate.opsForList().range(roomHistoryKey(roomId), 0, -1);
        } catch (DataAccessException exception) {
            logger.warn("Redis unavailable while reading room history. Using local room history.", exception);
            return new ArrayList<>(localHistory.getOrDefault(roomId, Collections.emptyList()));
        }

        List<DrawSegment> history = new ArrayList<>();
        if (rawSegments == null) return history;

        for (String rawSegment : rawSegments) {
            try {
                history.add(objectMapper.readValue(rawSegment, DrawSegment.class));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to read room history from Redis", exception);
            }
        }

        return history;
    }

    public void addToHistory(String roomId, DrawSegment segment) {
        localHistory.computeIfAbsent(roomId, key -> Collections.synchronizedList(new ArrayList<>())).add(segment);
        try {
            redisTemplate.opsForList().rightPush(roomHistoryKey(roomId), objectMapper.writeValueAsString(segment));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to store drawing segment in Redis", exception);
        } catch (DataAccessException exception) {
            logger.warn("Redis unavailable while storing drawing segment. Using local room history.", exception);
        }
    }

    public void clearHistory(String roomId) {
        localHistory.remove(roomId);
        try {
            redisTemplate.delete(roomHistoryKey(roomId));
        } catch (DataAccessException exception) {
            logger.warn("Redis unavailable while clearing room history. Cleared local room history only.", exception);
        }
    }

    public int getUserCount(String roomId) {
        try {
            Long size = redisTemplate.opsForSet().size(roomParticipantsKey(roomId));
            return size != null ? size.intValue() : 0;
        } catch (DataAccessException exception) {
            logger.warn("Redis unavailable while reading user count. Using local participant count.", exception);
            Set<String> participants = localParticipants.get(roomId);
            return participants != null ? participants.size() : 0;
        }
    }

    public void publishState(String roomId) {
        publish(roomId, "state", getHistory(roomId));
    }

    public void publishDraw(String roomId, DrawSegment segment) {
        publish(roomId, "draw", segment);
    }

    public void publishCursor(String roomId, CursorMessage cursor) {
        publish(roomId, "cursor", cursor);
    }

    public void publishClear(String roomId) {
        publish(roomId, "clear", "");
    }

    public void publishUsers(String roomId) {
        publish(roomId, "users", getUserCount(roomId));
    }

    public void publishCursorLeave(String roomId, String clientId) {
        publish(roomId, "cursor_leave", clientId);
    }

    private void publish(String roomId, String type, Object payload) {
        try {
            String event = objectMapper.writeValueAsString(Map.of(
                    "roomId", roomId,
                    "type", type,
                    "payload", payload
            ));
            redisTemplate.convertAndSend(roomEventsTopic.getTopic(), event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to publish Redis room event", exception);
        } catch (DataAccessException exception) {
            logger.warn("Redis unavailable while publishing room event. Broadcasting locally.", exception);
            publishLocally(roomId, type, payload);
        }
    }

    private void publishLocally(String roomId, String type, Object payload) {
        String destination = "/topic/room/" + roomId;
        switch (type) {
            case "state" -> messagingTemplate.convertAndSend(destination + "/state", payload);
            case "draw" -> messagingTemplate.convertAndSend(destination + "/draw", payload);
            case "cursor" -> messagingTemplate.convertAndSend(destination + "/cursor", payload);
            case "clear" -> messagingTemplate.convertAndSend(destination + "/clear", "");
            case "users" -> messagingTemplate.convertAndSend(destination + "/users", payload);
            case "cursor_leave" -> messagingTemplate.convertAndSend(destination + "/cursor-leave", payload);
            default -> {
            }
        }
    }

    private String roomHistoryKey(String roomId) {
        return "colaboard:room:" + roomId + ":history";
    }

    private String roomParticipantsKey(String roomId) {
        return "colaboard:room:" + roomId + ":participants";
    }

    public static class SessionInfo {
        private final String roomId;
        private final String clientId;
        private final String displayName;

        public SessionInfo(String roomId, String clientId, String displayName) {
            this.roomId = roomId;
            this.clientId = clientId;
            this.displayName = displayName;
        }

        public String getRoomId() {
            return roomId;
        }

        public String getClientId() {
            return clientId;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
