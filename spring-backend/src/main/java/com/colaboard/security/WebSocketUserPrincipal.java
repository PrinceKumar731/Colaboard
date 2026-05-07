package com.colaboard.security;

public class WebSocketUserPrincipal extends AuthenticatedUser {
    private final String websocketSessionId;

    public WebSocketUserPrincipal(String websocketSessionId, String id, String email, String displayName, String authProvider) {
        super(id, email, displayName, authProvider);
        this.websocketSessionId = websocketSessionId;
    }

    @Override
    public String getName() {
        return websocketSessionId;
    }

    public String getWebsocketSessionId() {
        return websocketSessionId;
    }
}
