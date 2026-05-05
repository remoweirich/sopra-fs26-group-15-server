package ch.uzh.ifi.hase.soprafs26.objects;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResult {
    private Long userId;
    private Integer totalPoints;
    private Integer roundPoints;
    private Long xCoordinate;
    private Long yCoordinate;
    private Double distance;
}