package com.colaboard.config;

import com.colaboard.model.UserAccount;
import com.colaboard.security.WebSocketUserPrincipal;
import com.colaboard.service.JwtService;
import com.colaboard.service.UserAccountService;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwtService;
    private final UserAccountService userAccountService;

    public WebSocketAuthChannelInterceptor(JwtService jwtService, UserAccountService userAccountService) {
        this.jwtService = jwtService;
        this.userAccountService = userAccountService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractBearer(accessor.getNativeHeader("Authorization"));
            if (token == null) {
                String sessionId = accessor.getSessionId();
                accessor.setUser(new WebSocketUserPrincipal(
                        sessionId,
                        "anon-" + sessionId,
                        "guest@local",
                        "Guest",
                        "ANONYMOUS"
                ));
                return message;
            }

            Claims claims = jwtService.parseToken(token);
            UserAccount user = userAccountService.findById(claims.getSubject());
            if (user == null) {
                throw new IllegalArgumentException("Invalid websocket authentication token.");
            }

            accessor.setUser(new WebSocketUserPrincipal(
                    accessor.getSessionId(),
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getAuthProvider()
            ));
        }
        return message;
    }

    private String extractBearer(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        String value = values.get(0);
        return value != null && value.startsWith("Bearer ") ? value.substring(7) : null;
    }
}
