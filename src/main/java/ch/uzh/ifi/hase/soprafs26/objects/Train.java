package ch.uzh.ifi.hase.soprafs26.objects;

import java.util.List;
import ch.uzh.ifi.hase.soprafs26.entity.LineString;

public class Train {

    private String trainId;
    private Line line;
    private String lineOrigin;
    private String lineDestination;
    private long departureTime;
    private long arrivalTime;
    private Station lastLeavingStation;
    private Station nextPendingStation;
    private long currentX;
    private long currentY;
    private long timestamp; 
    private LineString lineString; 

    public Train() {}

    public Train(String trainId) {
        this.trainId = trainId;
    }

    // --- Getters & Setters ---

    public String getTrainId() { return trainId; }
    public void setTrainId(String trainId) { this.trainId = trainId; }

    public Line getLine() { return line; }
    public void setLine(Line line) { this.line = line; }

    public String getLineOrigin() { return lineOrigin; }
    public void setLineOrigin(String lineOrigin) { this.lineOrigin = lineOrigin; }

    public String getLineDestination() { return lineDestination; }
    public void setLineDestination(String lineDestination) { this.lineDestination = lineDestination; }

    public long getDepartureTime() { return departureTime; }
    public void setDepartureTime(long departureTime) { this.departureTime = departureTime; }

    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long arrivalTime) { this.arrivalTime = arrivalTime; }

    public Station getLastLeavingStation() { return lastLeavingStation; }
    public void setLastLeavingStation(Station lastLeavingStation) { this.lastLeavingStation = lastLeavingStation; }

    public Station getNextPendingStation() { return nextPendingStation; }
    public void setNextPendingStation(Station nextPendingStation) { this.nextPendingStation = nextPendingStation; }

    public long getCurrentX() { return currentX; }
    public void setCurrentX(long currentX) { this.currentX = currentX; }

    public long getCurrentY() { return currentY; }
    public void setCurrentY(long currentY) { this.currentY = currentY; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public LineString getLineString() { return lineString; }
    public void setLineString(LineString lineString) { this.lineString = lineString;}
    @Override
    public String toString() {
        return "Train{id='" + trainId + "', line=" + line + ", origin='" + lineOrigin
                + "', destination='" + lineDestination + "', lastLeaving=" + lastLeavingStation
                + ", nextPending=" + nextPendingStation + ", currentX=" + currentX + ", currentY=" + currentY + ", timestamp=" + timestamp + ", lineStringPoints=" + (lineString != null ? lineString.getPoints().size() : 0) + "}";
    }

    // --- Nested Line class ---

    public static class Line {
        private String name;

        public Line() {}

        public Line(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public String toString() {
            return "Line{name='" + name + "'}";
        }
    }
}