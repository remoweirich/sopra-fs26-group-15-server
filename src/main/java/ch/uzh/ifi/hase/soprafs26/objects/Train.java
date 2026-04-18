package ch.uzh.ifi.hase.soprafs26.objects;

public class Train {

    private String trainId;
    private Line line;
    private Station lineOrigin;
    private Station lineDestination;
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

    // Copy constructor
    public Train(Train other) {
        this.trainId = other.trainId;
        this.line = other.line;
        this.lineOrigin = other.lineOrigin;
        this.lineDestination = other.lineDestination;
        this.departureTime = other.departureTime;
        this.arrivalTime = other.arrivalTime;
        this.lastLeavingStation = other.lastLeavingStation;
        this.nextPendingStation = other.nextPendingStation;
        this.currentX = other.currentX;
        this.currentY = other.currentY;
        this.timestamp = other.timestamp;
        this.lineString = other.lineString;
    }

    // --- Getters & Setters ---

    public String getTrainId() { return trainId; }
    public void setTrainId(String trainId) { this.trainId = trainId; }

    public Line getLine() { return line; }
    public void setLine(Line line) { this.line = line; }

    public Station getLineOrigin() { return lineOrigin; }
    public void setLineOrigin(Station lineOrigin) { this.lineOrigin = lineOrigin; }

    public Station getLineDestination() { return lineDestination; }
    public void setLineDestination(Station lineDestination) { this.lineDestination = lineDestination; }

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