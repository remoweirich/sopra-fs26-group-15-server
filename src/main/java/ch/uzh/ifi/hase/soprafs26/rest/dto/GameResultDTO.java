package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.objects.Round;
import ch.uzh.ifi.hase.soprafs26.objects.Score;
import ch.uzh.ifi.hase.soprafs26.objects.Train;

import java.util.List;
import java.util.Map;

public class GameResultDTO {

    private Long gameId;

    private List<Round> rounds;

    private List<Score> scores;

    private Map<Long, String> usernames;

    public Long getGameId() {return gameId;}
    public void setGameId(Long gameId) {this.gameId = gameId;}

    public List<Round> getRounds() {return rounds;}
    public void setRounds(List<Round> rounds) {this.rounds = rounds;}

    public List<Score> getScores() {return scores;}
    public void setScores(List<Score> scores) {this.scores = scores;}

    public Map<Long, String> getUsernames() {return usernames;}
    public void setUsernames(Map<Long, String> usernames) {this.usernames = usernames;}

}
