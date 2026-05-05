package ch.uzh.ifi.hase.soprafs26.rest.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuessMessageDTO {
    private Long lobbyId;
    private Long userId;
    private Long xCoordinate;
    private Long yCoordinate;
}