package ch.uzh.ifi.hase.soprafs26.entity;


import ch.uzh.ifi.hase.soprafs26.objects.Round;
import ch.uzh.ifi.hase.soprafs26.objects.Score;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


@Entity
@Table(name = "gameResult")
public class GameResult implements Serializable {

    @Id
    @Getter
    @Setter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long gameId;

    @Setter
    @Getter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private List<Round> rounds;

    @Setter
    @Getter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<Long, String> usernames;

    @Setter
    @Getter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private List<Score> scores;

}
