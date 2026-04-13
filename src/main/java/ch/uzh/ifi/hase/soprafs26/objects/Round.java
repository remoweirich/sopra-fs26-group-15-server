package ch.uzh.ifi.hase.soprafs26.objects;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Round {

    private int roundNumber;
    private Train train;

    private Map<Long, GuessMessageDTO> guessMessages;
    private Map<Long, UserGameStatus> allUserGameStatuses;
    private Map<Long, Score> scores;
    private Map<Long, Double> distances;

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


    public void setScore(Long userId, int points) {
        Score score = scores.get(userId);
        if (score != null) {
            score.setPoints(points);
        }
    }
    public Map<Long, Score> getAllScores() {
        return this.scores;
    }

    public void setUserGameStatus(Long userId, boolean isReady) {
        UserGameStatus status = allUserGameStatuses.get(userId);
        if (status != null) {
            status.setIsReady(isReady);
        }
    }
    public List<UserGameStatus> getAllUserGameStatuses() {
        return new ArrayList<>(allUserGameStatuses.values());
    }

    public void setDistances(long userId, double distance) {
        this.distances.put(userId, distance);
    }
    public Map<Long, Double> getDistances() {return this.distances;}

    public void setGuessMessage(Long userId, GuessMessageDTO guess) {
        guessMessages.put(userId, guess);
    }
    public Map<Long, GuessMessageDTO> getGuessMessages() {
        return this.guessMessages;
    }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }

}