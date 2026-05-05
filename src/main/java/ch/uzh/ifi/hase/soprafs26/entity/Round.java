package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "round")
@Getter
@Setter
@NoArgsConstructor
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roundId;

    @ManyToOne
    @JoinColumn(name = "idLobby", nullable = false)
    private Lobby lobby;

    @Column(nullable = false)
    private Integer roundNumber;

    @Column(columnDefinition = "TEXT")
    private String trainData;
}