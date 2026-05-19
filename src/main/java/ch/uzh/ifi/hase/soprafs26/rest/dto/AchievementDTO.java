package ch.uzh.ifi.hase.soprafs26.rest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AchievementDTO {
    private Long achievementId;
    private String name;
    private String description;
    private String iconUrl;
}
