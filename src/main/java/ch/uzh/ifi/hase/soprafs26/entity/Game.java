package ch.uzh.ifi.hase.soprafs26.entity;

import java.util.List;

public class Game {

    //randomized??
    private String gameID;

    private List<Round> rounds;

    private List<Train> trains;


//    public String getGameID() {return gameID;}
//    public void setGameID(String gameID) {this.gameID = gameID;}

    public List<Round> getRounds() {return rounds;}
    public void setRounds(List<Round> rounds) {this.rounds = rounds;}

    public List<Train> getTrains() {return trains;}
    public void setTrains(List<Train> trains) {this.trains = trains;}

}
