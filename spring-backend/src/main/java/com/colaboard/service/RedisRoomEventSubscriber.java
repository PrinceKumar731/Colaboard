package com.colaboard.service;

import com.colaboard.model.CursorMessage;
import com.colaboard.model.DrawSegment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RedisRoomEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public RedisRoomEventSubscriber(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String raw = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            JsonNode root = objectMapper.readTree(raw);
            String roomId = root.path("roomId").asText();
            String type = root.path("type").asText();
            JsonNode payload = root.path("payload");

            switch (type) {
                case "state" -> messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId + "/state",
                        objectMapper.convertValue(payload, new TypeReference<List<DrawSegment>>() {})
                );
                case "draw" -> messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId + "/draw",
                        objectMapper.treeToValue(payload, DrawSegment.class)
                );
                case "cursor" -> messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId + "/cursor",
                        objectMapper.treeToValue(payload, CursorMessage.class)
                );
                case "clear" -> messagingTemplate.convertAndSend("/topic/room/" + roomId + "/clear", "");
                case "users" -> messagingTemplate.convertAndSend("/topic/room/" + roomId + "/users", payload.asInt());
                case "cursor_leave" -> messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId + "/cursor-leave",
                        payload.asText()
                );
                default -> {
                }
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to process Redis room event", exception);
        }
    }
}
