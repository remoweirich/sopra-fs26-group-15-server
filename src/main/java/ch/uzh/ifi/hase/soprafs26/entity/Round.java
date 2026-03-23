package ch.uzh.ifi.hase.soprafs26.entity;

import java.util.List;

public class Round {

    private int roundNumber;

    private Train train;

    private List<GuessMessage> guessMessages;

    private List<UserGameStatus> allUserGameStatuses;


    public int getRoundNumber() {return roundNumber;}
    public void setRoundNumber(int roundNumber) {this.roundNumber = roundNumber;}

    public Train getTrain() {return train;}
    public void setTrain(Train train) {this.train = train;}

    public List<GuessMessage> getGuessMessages() {return guessMessages;}
    public void setGuessMessages(List<GuessMessage> guessMessages) {this.guessMessages = guessMessages;}

    public List<UserGameStatus> getAllUserGameStatuses() {return allUserGameStatuses;}
    public void setAllUserGameStatuses(List<UserGameStatus> allUserGameStatuses) {this.allUserGameStatuses = allUserGameStatuses;}
}
