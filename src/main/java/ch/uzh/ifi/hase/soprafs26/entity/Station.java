package ch.uzh.ifi.hase.soprafs26.entity;

public class Station {

    private String stationName;
    private long xCoordinate; // EPSG:3857
    private long yCoordinate; // EPSG:3857
    private long departureTime; // Unix ms

    public Station() {}

    public Station(String stationName, long xCoordinate, long yCoordinate, long departureTime) {
        this.stationName = stationName;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.departureTime = departureTime;
    }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public long getXCoordinate() { return xCoordinate; }
    public void setXCoordinate(long xCoordinate) { this.xCoordinate = xCoordinate; }

    public long getYCoordinate() { return yCoordinate; }
    public void setYCoordinate(long yCoordinate) { this.yCoordinate = yCoordinate; }

    public long getDepartureTime() { return departureTime; }
    public void setDepartureTime(long departureTime) { this.departureTime = departureTime; }

    @Override
    public String toString() {
        return "Station{name='" + stationName + "', x=" + xCoordinate + ", y=" + yCoordinate
                + ", departure=" + departureTime + "}";
    }
}