package ch.uzh.ifi.hase.soprafs26.entity;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private Boolean cleanupPending = false;

    @ManyToOne
    @JoinColumn(name = "admin", nullable = false)
    private User admin;

    @ManyToOne
    @JoinColumn(name = "winner", nullable = true)
    private User winner;

    @Column(nullable = false)
    private Integer maxPlayers;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @PrePersist
    protected void onCreate() {
        this.creationDate = LocalDateTime.now();
    }

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