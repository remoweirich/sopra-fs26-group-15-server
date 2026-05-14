package ch.uzh.ifi.hase.soprafs26.rest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequestDTO {
    private Long userId;
    private String username;
    private Long friendShipId;
}
