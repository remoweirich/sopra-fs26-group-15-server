package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GameDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import websocket.WebSocketConfig;


@Controller
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public GameController(GameService gameService, SimpMessagingTemplate simpMessagingTemplate) {
        this.gameService = gameService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @MessageMapping("/game/{gameId}/guess")
    public void processGuessMessage(@DestinationVariable Long gameId, GuessMessageDTO guessMessageDTO) {
        gameService.processGuessMessage(guessMessageDTO);
    }

    @MessageMapping("/game/{gameId}/ready")
    public void updateUserGameStatus(@DestinationVariable Long gameID, UserGameStatus userGameStatus) {
        gameService.updateUserGameStatus(gameID, userGameStatus);
    }
}
