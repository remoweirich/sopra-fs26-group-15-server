package ch.uzh.ifi.hase.soprafs26.entity;

public class GuessMessage {

    private String lobbyId;

    private String userId;

    private String latitude;

    private String longitude;


    public String getLobbyId() {return lobbyId;}
    public void setLobbyId(String lobbyId) {this.lobbyId = lobbyId;}

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}

    public String getLatitude() {return latitude;}
    public void setLatitude(String latitude) {this.latitude = latitude;}

    public String getLongitude() {return longitude;}
    public void setLongitude(String longitude) {this.longitude = longitude;}

}
