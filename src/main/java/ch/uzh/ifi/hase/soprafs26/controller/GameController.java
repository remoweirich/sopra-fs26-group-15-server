package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import tools.jackson.databind.ObjectMapper;


@Controller
public class GameController {

    private final GameService gameService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    public GameController(GameService gameService, SimpMessagingTemplate simpMessagingTemplate, ObjectMapper objectMapper) {
        this.gameService = gameService;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.objectMapper = objectMapper;
    }

    @MessageMapping("/game/{gameId}/guess")
    public void processGuessMessage(@DestinationVariable Long gameId, Message guessMessage) {
        MessageType type = guessMessage.getType();
        if (type == MessageType.GUESS_MESSAGE){
            GuessMessageDTO guessMessageDTO = objectMapper.convertValue(guessMessage.getPayload(), GuessMessageDTO.class);
            gameService.processGuessMessage(guessMessageDTO);
        }
    }

    @MessageMapping("/game/{gameId}/ready")
    public void updateUserGameStatus(@DestinationVariable Long gameID, Message readyForNextRoundMessage) {
        gameService.updateUserGameStatus(gameID, readyForNextRoundMessage);
    }
}
