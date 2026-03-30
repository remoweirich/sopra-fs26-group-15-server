package ch.uzh.ifi.hase.soprafs26.objects;

public class GuessMessage {

    private Long lobbyId;

    private Long userId;

    private String latitude;

    private String longitude;


    public GuessMessage(Long lobbyId, Long userId) {
        this.lobbyId = lobbyId;
        this.userId = userId;
    }

    public Long getLobbyId() {return lobbyId;}
    public void setLobbyId(Long lobbyId) {this.lobbyId = lobbyId;}

    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}

    public String getLatitude() {return latitude;}
    public void setLatitude(String latitude) {this.latitude = latitude;}

    public String getLongitude() {return longitude;}
    public void setLongitude(String longitude) {this.longitude = longitude;}

}
