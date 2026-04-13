package ch.uzh.ifi.hase.soprafs26.objects;

import lombok.Getter;

public class UserResult {
    @Getter
    private long userId;

    @Getter
    private int totalPoints;

    @Getter
    private int roundPoints;

    @Getter
    private long xCoordinate;

    @Getter
    private long yCoordinate;

    @Getter
    private double distance;

    public UserResult(long userId, int totalPoints, int roundPoints, long xCoordinate, long yCoordinate, double distance) {
        this.userId = userId;
        this.totalPoints = totalPoints;
        this.roundPoints = roundPoints;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.distance = distance;
    }

}
