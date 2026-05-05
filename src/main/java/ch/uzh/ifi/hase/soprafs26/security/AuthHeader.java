package ch.uzh.ifi.hase.soprafs26.security;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthHeader {
    private Long userId;
    private String token;
}