package ch.uzh.ifi.hase.soprafs26.rest.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GameResultDTO {
    private Long gameId;
    private List<RoundResultDTO> rounds;
    private List<ScoreDTO> scores;
    private Map<Long, String> usernames;
}