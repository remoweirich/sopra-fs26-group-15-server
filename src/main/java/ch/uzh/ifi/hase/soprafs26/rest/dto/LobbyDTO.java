package ch.uzh.ifi.hase.soprafs26.rest.dto;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import lombok.*;

@Getter
@Setter
public class LobbyDTO {
    private Long lobbyId;
    private String lobbyName;
    private Integer maxPlayers;
    private Integer currentPlayers;  // berechnet aus players.size()
    private LobbyVisibility visibility;
    private Integer maxRounds;
    private LobbyState lobbyState;
    private String lobbyCode;
}