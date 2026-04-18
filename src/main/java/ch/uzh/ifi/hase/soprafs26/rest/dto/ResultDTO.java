package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.objects.Score;
import ch.uzh.ifi.hase.soprafs26.objects.Train;
import ch.uzh.ifi.hase.soprafs26.objects.UserResult;

import java.util.List;

public class ResultDTO {
    private int currentRound;

    private List<UserResult> userResults;

    private Train train;

    public ResultDTO(int currentRound, List<UserResult> userResults, Train train) {
        this.currentRound = currentRound;
        this.userResults = userResults;
        this.train = train;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public List<UserResult> getUserResults() {
        return userResults;
    }

    public void setUserResults(List<UserResult> userResults) {
        this.userResults = userResults;
    }

    public Train getTrain() {
        return train;
    }

    public void setTrain(Train train) {
        this.train = train;
    }
}
