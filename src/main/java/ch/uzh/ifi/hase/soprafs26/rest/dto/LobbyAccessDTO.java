package ch.uzh.ifi.hase.soprafs26.rest.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LobbyAccessDTO {
    private Long lobbyId;
    private String lobbyCode;
    private Long userId;
    private String token;
}