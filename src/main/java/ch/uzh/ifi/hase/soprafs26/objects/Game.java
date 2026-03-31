package ch.uzh.ifi.hase.soprafs26.objects;

import ch.uzh.ifi.hase.soprafs26.objects.Round;
import ch.uzh.ifi.hase.soprafs26.objects.Train;

import java.util.List;

public class Game {


    private Long gameID;

    private List<Round> rounds;

    private List<Train> trains;


    public Game(Long gameID, List<Round> rounds, List<Train> trains) {
        this.gameID = gameID;
        this.rounds = rounds;
        this.trains = trains;
    }

    public Long getGameID() {return gameID;}
    public void setGameID(Long gameID) {this.gameID = gameID;}

    public List<Round> getRounds() {return rounds;}
    public void setRounds(List<Round> rounds) {this.rounds = rounds;}

    public List<Train> getTrains() {return trains;}
    public void setTrains(List<Train> trains) {this.trains = trains;}

}
