package ch.uzh.ifi.hase.soprafs26.objects;

public class Station {

    private String stationName;
    private long xCoordinate; // EPSG:3857
    private long yCoordinate; // EPSG:3857
    private long departureTime; // Unix ms
    private long arrivalTime; // Unix ms 

    public Station() {}

    public Station(String stationName, long xCoordinate, long yCoordinate, long departureTime, long arrivalTime) {
        this.stationName = stationName;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public long getXCoordinate() { return xCoordinate; }
    public void setXCoordinate(long xCoordinate) { this.xCoordinate = xCoordinate; }

    public long getYCoordinate() { return yCoordinate; }
    public void setYCoordinate(long yCoordinate) { this.yCoordinate = yCoordinate; }

    public long getDepartureTime() { return departureTime; }
    public void setDepartureTime(long departureTime) { this.departureTime = departureTime; }

    public long getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(long arrivalTime) { this.arrivalTime = arrivalTime;
    }
    @Override
    public String toString() {
        return "Station{name='" + stationName + "', x=" + xCoordinate + ", y=" + yCoordinate
                + ", departure=" + departureTime + "}";
    }
}