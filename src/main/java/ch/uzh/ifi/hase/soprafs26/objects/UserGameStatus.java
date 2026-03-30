package ch.uzh.ifi.hase.soprafs26.objects;

public class UserGameStatus {

    private Long userId;

    private Boolean isReady;


    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}

    public Boolean getIsReady() {return isReady;}
    public void setIsReady(Boolean isReady) {this.isReady = isReady;}
}
