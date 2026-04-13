package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.objects.Train;

public class RoundStartDTO {
    private Integer roundNumber;

    private Integer maxRounds;

    private Train train;


    public RoundStartDTO(int roundNumber, int maxRounds, Train train){
        this.roundNumber = roundNumber;
        this.maxRounds = maxRounds;
        this.train = train;
    }

    public Integer getRoundNumber() {return roundNumber;}
    public void setRoundNumber(Integer roundNumber) {this.roundNumber = roundNumber;}

    public Integer getMaxRounds() {return maxRounds;}
    public void setMaxRounds(Integer maxRounds) {this.maxRounds = maxRounds;}

    public Train getTrain() {return train;}
    public void setTrain(Train train) {this.train = train;}
}
