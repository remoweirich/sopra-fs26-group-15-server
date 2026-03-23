package ch.uzh.ifi.hase.soprafs26.entity;

public class UserScoreboard {

    private Integer totalPoints;

    private Integer gamesPlayed;

    private Integer gamesWon;

    private Float guessingPrecision;


    public Integer getTotalPoints() {return totalPoints;}
    public void setTotalPoints(Integer totalPoints) {this.totalPoints = totalPoints;}

    public Integer getGamesPlayed() {return gamesPlayed;}
    public void setGamesPlayed(Integer gamesPlayed) {this.gamesPlayed = gamesPlayed;}

    public Integer getGamesWon() {return gamesWon;}
    public void setGamesWon(Integer gamesWon) {this.gamesWon = gamesWon;}

    public Float getGuessingPrecision() {return guessingPrecision;}
    public void setGuessingPrecision(Float guessingPrecision) {this.guessingPrecision = guessingPrecision;}
}
