package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.entity.Achievement;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserAchievementDTO {
    private Achievement achievement;
    private LocalDateTime unlockedAt;
    private Long userId;
}
