package ch.uzh.ifi.hase.soprafs26.objects;

import java.util.Date;

public class Train {

    private String trainName;

    //private ?? coordinates;

    private Date departureTime;

    private Date arrivalTime;


    public String getTrainName() {return trainName;}
    public void setTrainName(String trainName) {this.trainName = trainName;}

    //public ?? getCoordinates() {return coordinates;}
    //public void setCoordinates(?? coordinates) {this.coordinates = coordinates}

    public Date getDepartureTime() {return departureTime;}
    public void setDepartureTime(Date departureTime) {this.departureTime = departureTime;}

    public Date getArrivalTime() {return arrivalTime;}
    public void setArrivalTime(Date arrivalTime) {this.arrivalTime = arrivalTime;}
}
