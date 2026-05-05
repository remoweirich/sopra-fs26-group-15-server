package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roundHistory")
@Getter
@Setter
@NoArgsConstructor
public class RoundHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roundHistoryId;

    @ManyToOne
    @JoinColumn(name = "idLobby", nullable = false)
    private Lobby lobby;

    @ManyToOne
    @JoinColumn(name = "idUser", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private Float distanceToTrain;
}