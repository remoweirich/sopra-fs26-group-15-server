package ch.uzh.ifi.hase.soprafs26.objects;

import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Round {

    private int roundNumber;
    private Train train;

    private Map<Long, GuessMessageDTO> guessMessages;
    private Map<Long, UserGameStatus> allUserStatuses;
    private Map<Long, Score> scores;

    public Round(int roundNumber, Train train,
                 Map<Long, GuessMessageDTO> guessMessages,
                 Map<Long, UserGameStatus> allUserStatuses,
                 Map<Long, Score> scores) {

        this.roundNumber = roundNumber;
        this.train = train;
        this.guessMessages = guessMessages;
        this.allUserStatuses = allUserStatuses;
        this.scores = scores;
    }

    // --- Clean API ---

    public void setScore(Long userId, int points) {
        Score score = scores.get(userId);
        if (score != null) {
            score.setPoints(points);
        }
    }
    public List<Score> getAllScores() {
        return new ArrayList<>(scores.values());
    }

    public void setUserStatus(Long userId, boolean isReady) {
        UserGameStatus status = allUserStatuses.get(userId);
        if (status != null) {
            status.setIsReady(isReady);
        }
    }
    public List<UserGameStatus> getAllUserStatuses() {
        return new ArrayList<>(allUserStatuses.values());
    }

    public void setGuessMessage(Long userId, GuessMessageDTO guess) {
        guessMessages.put(userId, guess);
    }
    public List<GuessMessageDTO> getGuessMessages() {
        return new ArrayList<>(guessMessages.values());
    }

    public int getRoundNumber() { return roundNumber; }
    public void setRoundNumber(int roundNumber) { this.roundNumber = roundNumber; }
    public Train getTrain() { return train; }
    public void setTrain(Train train) { this.train = train; }

}