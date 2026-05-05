package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import lombok.*;

@Getter
@Setter
public class UpdateLobbyDTO {
    private String lobbyName;
    private Integer maxPlayers;
    private LobbyVisibility visibility;
    private Integer maxRounds;
}