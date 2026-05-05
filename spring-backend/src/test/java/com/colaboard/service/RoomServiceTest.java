package com.colaboard.service;

import com.colaboard.model.DrawSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomServiceTest {

    @Test
    void tracksSessionsPerRoomAndReturnsDisconnectInfo() {
        RoomService roomService = new RoomService();

        roomService.addSession("ROOM1", "session-a", "client-a");
        roomService.addSession("ROOM1", "session-b", "client-b");

        assertEquals(2, roomService.getUserCount("ROOM1"));

        RoomService.SessionInfo removed = roomService.removeSession("session-a");

        assertNotNull(removed);
        assertEquals("ROOM1", removed.getRoomId());
        assertEquals("client-a", removed.getClientId());
        assertEquals(1, roomService.getUserCount("ROOM1"));
    }

    @Test
    void returnsSnapshotCopyOfRoomHistory() {
        RoomService roomService = new RoomService();
        DrawSegment segment = new DrawSegment();
        segment.setId("el-1");

        roomService.addToHistory("ROOM2", segment);
        List<DrawSegment> history = roomService.getHistory("ROOM2");

        assertEquals(1, history.size());
        assertTrue(history.stream().anyMatch(item -> "el-1".equals(item.getId())));
    }
}
