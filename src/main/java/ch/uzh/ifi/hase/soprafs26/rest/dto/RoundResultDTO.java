package ch.uzh.ifi.hase.soprafs26.rest.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class RoundResultDTO {
    private Integer roundNumber;
    private Map<Long, Integer> scores;      // userId → points
    private Map<Long, Double> distances;    // userId → distance
}