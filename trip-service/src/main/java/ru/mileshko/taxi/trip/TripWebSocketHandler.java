package ru.mileshko.taxi.trip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
class TripWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(TripWebSocketHandler.class);
    private final Map<String, Set<WebSocketSession>> tripSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    TripWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String tripId = extractTripId(session);
        if (tripId == null) {
            closeSession(session, CloseStatus.BAD_DATA);
            return;
        }
        tripSessions.computeIfAbsent(tripId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket connected: tripId={}, sessionId={}", tripId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tripId = extractTripId(session);
        if (tripId != null) {
            Set<WebSocketSession> sessions = tripSessions.get(tripId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    tripSessions.remove(tripId);
                }
            }
        }
        log.info("WebSocket disconnected: tripId={}, sessionId={}", tripId, session.getId());
    }

    void broadcastStatusChange(long tripId, TripStatus status) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "STATUS_CHANGE",
                    "tripId", tripId,
                    "status", status.name(),
                    "timestamp", Instant.now().toString()
            ));
            TextMessage message = new TextMessage(payload);
            Set<WebSocketSession> sessions = tripSessions.get(String.valueOf(tripId));
            if (sessions == null || sessions.isEmpty()) {
                return;
            }
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("Failed to send WS message to sessionId={}: {}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize WS message for tripId={}", tripId, e);
        }
    }

    private String extractTripId(WebSocketSession session) {
        if (session.getUri() == null) return null;
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException e) {
            log.warn("Failed to close WS session: {}", e.getMessage());
        }
    }
}
