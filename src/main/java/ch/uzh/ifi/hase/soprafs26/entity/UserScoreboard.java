package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;

@Embeddable
public class UserScoreboard {

    @Column(nullable = true)
    private Integer totalPoints;

    @Column(nullable = true)
    private Integer gamesPlayed;

    @Column(nullable = true)
    private Integer gamesWon;

    @Column(nullable = true)
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
