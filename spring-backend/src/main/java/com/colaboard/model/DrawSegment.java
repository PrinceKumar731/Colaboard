package com.colaboard.model;

import java.util.List;

public class DrawSegment {
    private double prevX;
    private double prevY;
    private double x;
    private double y;
    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private String color;
    private String fillColor;
    private int lineWidth;
    private String tool;
    private String elementType;
    private String id;
    private List<Double> points;

    public DrawSegment() {}

    public double getPrevX() { return prevX; }
    public void setPrevX(double prevX) { this.prevX = prevX; }

    public double getPrevY() { return prevY; }
    public void setPrevY(double prevY) { this.prevY = prevY; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getStartX() { return startX; }
    public void setStartX(double startX) { this.startX = startX; }

    public double getStartY() { return startY; }
    public void setStartY(double startY) { this.startY = startY; }

    public double getEndX() { return endX; }
    public void setEndX(double endX) { this.endX = endX; }

    public double getEndY() { return endY; }
    public void setEndY(double endY) { this.endY = endY; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getFillColor() { return fillColor; }
    public void setFillColor(String fillColor) { this.fillColor = fillColor; }

    public int getLineWidth() { return lineWidth; }
    public void setLineWidth(int lineWidth) { this.lineWidth = lineWidth; }

    public String getTool() { return tool; }
    public void setTool(String tool) { this.tool = tool; }

    public String getElementType() { return elementType; }
    public void setElementType(String elementType) { this.elementType = elementType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Double> getPoints() { return points; }
    public void setPoints(List<Double> points) { this.points = points; }
}
