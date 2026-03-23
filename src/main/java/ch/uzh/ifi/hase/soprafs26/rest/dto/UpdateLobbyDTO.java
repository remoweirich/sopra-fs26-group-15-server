package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;

public class UpdateLobbyDTO {
    private String lobbyName;

    private Integer size;

    private LobbyVisibility lobbyVisibility;

    private Integer maxRounds;


    public String getLobbyName() {return lobbyName;}
    public void setLobbyName(String lobbyName) {this.lobbyName = lobbyName;}

    public Integer getSize() {return size;}
    public void setSize(Integer size) {this.size = size;}

    public LobbyVisibility getLobbyVisibility() {return lobbyVisibility;}
    public void setLobbyVisibility(LobbyVisibility lobbyVisibility) {this.lobbyVisibility = lobbyVisibility;}

    public Integer getMaxRounds() {return maxRounds;}
    public void setMaxRounds(Integer maxRounds) {this.maxRounds = maxRounds;}

}
