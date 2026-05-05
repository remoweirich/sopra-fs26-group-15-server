package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class UserScoreboard {

    @Column
    private Long totalPoints;

    @Column
    private Long playedGames;

    @Column
    private Long playedRounds;

    @Column
    private Long bestRoundPoints;

    @Column
    private Float guessingPrecision;
}