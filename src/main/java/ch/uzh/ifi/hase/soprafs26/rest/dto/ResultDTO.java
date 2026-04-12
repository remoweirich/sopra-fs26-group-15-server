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

}
