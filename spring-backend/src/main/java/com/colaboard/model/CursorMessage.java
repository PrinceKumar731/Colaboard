package com.colaboard.model;

public class CursorMessage {
    private String sessionId;
    private double x;
    private double y;

    public CursorMessage() {}

    public CursorMessage(String sessionId, double x, double y) {
        this.sessionId = sessionId;
        this.x = x;
        this.y = y;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
