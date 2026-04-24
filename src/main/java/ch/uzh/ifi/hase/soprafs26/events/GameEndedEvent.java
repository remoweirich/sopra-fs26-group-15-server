package ch.uzh.ifi.hase.soprafs26.events;

import org.springframework.context.ApplicationEvent;

public class GameEndedEvent extends ApplicationEvent {
    private final Long gameId;

    public GameEndedEvent(Object source, Long gameId) {
        super(source);
        this.gameId = gameId;
    }

    public Long getGameId() {
        return gameId;
    }
}
