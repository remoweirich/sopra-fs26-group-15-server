package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;

public class LobbyDTO {
    private String lobbyName;

    private Integer size;

    private LobbyVisibility visibility;

    private Integer maxRounds;

    private LobbyState lobbyState;

    private String lobbyCode;

    private Long lobbyId;


    public String getLobbyName() {return lobbyName;}
    public void setLobbyName(String lobbyName) {this.lobbyName = lobbyName;}

    public Integer getSize() {return size;}
    public void setSize(Integer size) {this.size = size;}

    public LobbyVisibility getVisibility() {return visibility;}
    public void setVisibility(LobbyVisibility visibility) {this.visibility = visibility;}

    public Integer getMaxRounds() {return maxRounds;}
    public void setMaxRounds(Integer maxRounds) {this.maxRounds = maxRounds;}

    public LobbyState getLobbyState() {return lobbyState;}
    public void setLobbyState(LobbyState lobbyState) {this.lobbyState = lobbyState;}

    public String getLobbyCode() {return lobbyCode;}
    public void setLobbyCode(String lobbyCode) {this.lobbyCode = lobbyCode;}

    public Long getLobbyId() {return lobbyId;}
    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

}
