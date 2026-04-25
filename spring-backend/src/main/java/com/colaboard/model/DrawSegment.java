package com.colaboard.model;

public class DrawSegment {
    private double prevX;
    private double prevY;
    private double x;
    private double y;
    private String color;
    private int lineWidth;
    private String tool;

    public DrawSegment() {}

    public double getPrevX() { return prevX; }
    public void setPrevX(double prevX) { this.prevX = prevX; }

    public double getPrevY() { return prevY; }
    public void setPrevY(double prevY) { this.prevY = prevY; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public int getLineWidth() { return lineWidth; }
    public void setLineWidth(int lineWidth) { this.lineWidth = lineWidth; }

    public String getTool() { return tool; }
    public void setTool(String tool) { this.tool = tool; }
}
