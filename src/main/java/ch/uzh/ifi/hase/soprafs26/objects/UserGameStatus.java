package ch.uzh.ifi.hase.soprafs26.objects;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserGameStatus {
    private Long userId;
    private Boolean isReady;
}