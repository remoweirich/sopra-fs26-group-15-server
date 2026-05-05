package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lobby")
@Getter
@Setter
@NoArgsConstructor
public class Lobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lobbyId;

    @Column(nullable = false, length = 255)
    private String lobbyName;

    @Column(nullable = false, length = 255)
    private String lobbyCode;

    @ManyToOne
    @JoinColumn(name = "admin", nullable = false)
    private User admin;

    @Column(nullable = false)
    private Integer maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LobbyVisibility visibility;

    @Column(nullable = false)
    private Integer maxRounds;

    @Column(nullable = false)
    private Integer currentRound = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LobbyState lobbyState;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "lobbyPlayer",
            joinColumns = @JoinColumn(name = "idLobby"),
            inverseJoinColumns = @JoinColumn(name = "idUser")
    )
    private List<User> players = new ArrayList<>();
}