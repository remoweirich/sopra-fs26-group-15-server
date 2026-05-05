package ch.uzh.ifi.hase.soprafs26.rest.dto;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class MyUserDTO {
    private Long userId;
    private String username;
    private String email;
    private String userBio;
    private LocalDateTime creationDate;
    private Boolean isGuest;
    private Boolean isOnline;
    private UserScoreboardDTO userScoreboard;
    private List<UserDTO> friends;
}