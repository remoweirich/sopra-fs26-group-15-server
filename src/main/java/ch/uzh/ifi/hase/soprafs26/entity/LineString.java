package ch.uzh.ifi.hase.soprafs26.entity;

import java.util.List;
public class LineString {
    
    private List<Point> points;
    
    public LineString() {}

    public LineString(List<Point> points) {
        this.points = points;
    }

    public List<Point> getPoints() { return points; }
    public void setPoints(List<Point> points) { this.points = points; }

    public static class Point {
        private long x;
        private long y;

        public Point() {}

        public Point(long x, long y) {
            this.x = x;
            this.y = y;
        }

        public long getX() { return x; }
        public void setX(long x) { this.x = x; }

        public long getY() { return y; }
        public void setY(long y) { this.y = y; }

        @Override
        public String toString() {
            return "Point{x=" + x + ", y=" + y + "}";
        }
    }
}
