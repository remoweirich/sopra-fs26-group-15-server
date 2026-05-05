package ch.uzh.ifi.hase.soprafs26.rest.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserScoreboardDTO {
    private Long totalPoints;
    private Long playedGames;
    private Long playedRounds;
    private Long bestRoundPoints;
    private Float guessingPrecision;
}