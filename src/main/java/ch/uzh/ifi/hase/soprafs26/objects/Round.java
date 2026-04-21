package ch.uzh.ifi.hase.soprafs26.objects;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Round {

    @Getter
    @Setter
    private int roundNumber;
    @Setter
    @Getter
    private Train train;

    @Getter
    @Setter
    private Map<Long, GuessMessageDTO> guessMessages;

    @Getter
    @Setter
    private Map<Long, UserGameStatus> allUserGameStatuses;

    @Getter
    @Setter
    private Map<Long, Score> scores;

    @Getter
    @Setter
    private Map<Long, Double> distances;

    public Round(){}

    public Round(int roundNumber, Train train,
                 Map<Long, GuessMessageDTO> guessMessages,
                 Map<Long, UserGameStatus> allUserGameStatuses,
                 Map<Long, Score> scores,
                 Map<Long, Double> distances) {

        this.roundNumber = roundNumber;
        this.train = train;
        this.guessMessages = guessMessages;
        this.allUserGameStatuses = allUserGameStatuses;
        this.scores = scores;
        this.distances = distances;
    }

    @JsonIgnore
    public void setScore(Long userId, int points) {
        Score score = scores.get(userId);
        if (score != null) {
            score.setPoints(points);
        }
    }

    @JsonIgnore
    public void setUserGameStatus(Long userId, boolean isReady) {
        UserGameStatus status = allUserGameStatuses.get(userId);
        if (status != null) {
            status.setIsReady(isReady);
        }
    }

    @JsonIgnore
    public List<UserGameStatus> getAllUserGameStatusesList() {
        return new ArrayList<>(allUserGameStatuses.values());
    }

    @JsonIgnore
    public void setDistances(long userId, double distance) {
        this.distances.put(userId, distance);
    }

    @JsonIgnore
    public void setGuessMessage(Long userId, GuessMessageDTO guess) {
        guessMessages.put(userId, guess);
    }


}