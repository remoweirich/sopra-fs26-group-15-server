package ch.uzh.ifi.hase.soprafs26.rest.dto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class UserDTO {
    private Long userId;
    private String username;
    private String userBio;
    private LocalDateTime creationDate;
    private UserScoreboardDTO userScoreboard;
}