package ch.uzh.ifi.hase.soprafs26.objects;

public class Score {

    private Long userId;

    private Integer points;

    public Score() {}

    public Score(Long userId) {
            this.userId = userId;
        }

    public Long getUserId() {return userId;}
    public void setUserId(Long userId) {this.userId = userId;}

    public Integer getPoints() {return points;}
    public void setPoints(Integer points) {this.points = points;}
}
