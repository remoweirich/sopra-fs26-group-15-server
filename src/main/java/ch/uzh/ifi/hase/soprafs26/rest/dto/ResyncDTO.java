package ch.uzh.ifi.hase.soprafs26.rest.dto;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResyncDTO {
    private MessageType type;
    private Object payload;
    private long remainingTime;
    private int maxRounds;
}