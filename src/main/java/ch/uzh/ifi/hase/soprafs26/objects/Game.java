package ch.uzh.ifi.hase.soprafs26.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {

    private Long gameId;

    private List<Round> rounds;

    private List<Train> trains;

    private Map<Long, UserGameStatus> connectedPlayers = new HashMap<>(); //To see if all players are Subscribed to the topic game/{gameId}

    public Game() {

    }

    public Game(Long gameId, List<Round> rounds, List<Train> trains,  Map<Long, UserGameStatus> connectedPlayers) {
        this.gameId = gameId;
        this.rounds = rounds;
        this.trains = trains;
    }

    public Long getGameId() {return gameId;}
    public void setGameId(Long gameId) {this.gameId = gameId;}

    public List<Round> getRounds() {return rounds;}
    public void setRounds(List<Round> rounds) {this.rounds = rounds;}

    public List<Train> getTrains() {return trains;}
    public void setTrains(List<Train> trains) {this.trains = trains;}

    public List<UserGameStatus> getConnectedPlayers() {
        return new ArrayList<>(connectedPlayers.values());
    }
    public void setConnectedPlayers(long userId, UserGameStatus userGameStatus) {
        connectedPlayers.put(userId, userGameStatus);
    }

}
