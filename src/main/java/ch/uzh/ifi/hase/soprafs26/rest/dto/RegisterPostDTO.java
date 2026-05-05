package ch.uzh.ifi.hase.soprafs26.rest.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterPostDTO {
    private String username;
    private String email;
    private String password;
    private String userBio;
    private Boolean isGuest;
}