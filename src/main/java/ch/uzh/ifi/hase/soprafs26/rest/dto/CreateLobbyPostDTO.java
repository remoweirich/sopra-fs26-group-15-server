package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;

public class CreateLobbyPostDTO {

    private Long userId;

    private String token;

    private String lobbyName;

    private Integer size;

    private LobbyVisibility visibility;

    private Integer maxRounds;


    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}

    public String getToken() {return token;}
    public void setToken(String token) {this.token = token;}

    public String getLobbyName() {return lobbyName;}
    public void setLobbyName(String lobbyName) {this.lobbyName = lobbyName;}

    public Integer getSize() {return size;}
    public void setSize(Integer size) {this.size = size;}

    public LobbyVisibility getVisibility() {return visibility;}
    public void setVisibility(LobbyVisibility visibility) {this.visibility = visibility;}

    public Integer getMaxRounds() {return maxRounds;}
    public void setMaxRounds(Integer maxRounds) {this.maxRounds = maxRounds;}
}
