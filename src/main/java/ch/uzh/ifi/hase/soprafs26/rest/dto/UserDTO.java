package ch.uzh.ifi.hase.soprafs26.rest.dto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UserDTO {
    private Long userId;
    private String username;
    private String userBio;
    private LocalDateTime creationDate;
    private UserScoreboardDTO userScoreboard;
    private List<UserAchievementDTO> userAchievementDTOList;
}