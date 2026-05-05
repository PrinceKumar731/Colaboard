package com.colaboard.model;

public class CursorMessage {
    private String sessionId;
    private String displayName;
    private double x;
    private double y;

    public CursorMessage() {}

    public CursorMessage(String sessionId, String displayName, double x, double y) {
        this.sessionId = sessionId;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
