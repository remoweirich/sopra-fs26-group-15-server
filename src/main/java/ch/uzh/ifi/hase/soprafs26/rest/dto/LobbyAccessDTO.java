package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class LobbyAccessDTO {

    private Long lobbyId;

    private String lobbyCode;

    private Long userId;

    private String token;

    public LobbyAccessDTO(Long lobbyId, String lobbyCode){
        this.lobbyId = lobbyId;
        this.lobbyCode = lobbyCode;
    }


    public Long getLobbyId() {return lobbyId;}
    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

    public String getLobbyCode() {return lobbyCode;}
    public void setLobbyCode(String lobbyCode) {this.lobbyCode = lobbyCode;}

    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}

    public String getToken() {return token;}
    public void setToken(String token) {this.token = token;}

}
