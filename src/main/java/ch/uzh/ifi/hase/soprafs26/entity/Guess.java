package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "guess", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"idRound", "idUser"})
})
@Getter
@Setter
@NoArgsConstructor
public class Guess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "idRound", nullable = false)
    private Round round;

    @ManyToOne
    @JoinColumn(name = "idUser", nullable = false)
    private User user;

    @Column
    private Float lat;

    @Column
    private Float lon;

    @Column
    private Integer points;

    @Column
    private Float distanceToTrain;

    @Column(nullable = false)
    private Boolean hasGuessed = false;
}