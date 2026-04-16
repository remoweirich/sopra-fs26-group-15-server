package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;

public class CreateLobbyPostDTO {

    private String lobbyName;

    private Integer size;

    private LobbyVisibility visibility;

    private Integer maxRounds;


    public String getLobbyName() {return lobbyName;}
    public void setLobbyName(String lobbyName) {this.lobbyName = lobbyName;}

    public Integer getSize() {return size;}
    public void setSize(Integer size) {this.size = size;}

    public LobbyVisibility getVisibility() {return visibility;}
    public void setVisibility(LobbyVisibility visibility) {this.visibility = visibility;}

    public Integer getMaxRounds() {return maxRounds;}
    public void setMaxRounds(Integer maxRounds) {this.maxRounds = maxRounds;}
}
