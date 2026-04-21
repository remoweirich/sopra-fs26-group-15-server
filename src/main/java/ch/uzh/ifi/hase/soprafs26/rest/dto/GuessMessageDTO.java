package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class GuessMessageDTO {

    private Long lobbyId;

    private Long userId;

    private Long Xcoordinate;

    private Long Ycoordinate;

    public GuessMessageDTO() {}

    public Long getLobbyId() {return lobbyId;}
    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}

    public Long getXcoordinate() {return Xcoordinate;}
    public void setXcoordinate(Long Xcoordinate) {this.Xcoordinate = Xcoordinate;}

    public Long getYcoordinate() {return Ycoordinate;}
    public void setYcoordinate(Long Ycoordinate) {this.Ycoordinate = Ycoordinate;}


    public GuessMessageDTO(long lobbyId, long userId) {
        this.lobbyId = lobbyId;
        this.userId = userId;
    }
}

