package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.constant.MessageType;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import tools.jackson.databind.ObjectMapper;


@Controller
public class GameController {

    private final GameService gameService;
    private final ObjectMapper objectMapper;
    private final LobbyService lobbyService;

    public GameController(GameService gameService, ObjectMapper objectMapper, LobbyService lobbyService) {
        this.gameService = gameService;
        this.objectMapper = objectMapper;
        this.lobbyService = lobbyService;
    }

    @MessageMapping("/game/{gameId}/guess")
    public void processGuessMessage(@DestinationVariable Long gameId, Message guessMessage) {
        Lobby currentLobby = lobbyService.getLobbyById(gameId);
        MessageType type = guessMessage.getType();
        if (type == MessageType.GUESS_MESSAGE){
            GuessMessageDTO guessMessageDTO = objectMapper.convertValue(guessMessage.getPayload(), GuessMessageDTO.class);
            gameService.processGuessMessage(guessMessageDTO, currentLobby);
        }
    }

    @MessageMapping("/game/{gameId}/ready")
    public void updateUserGameStatus(@DestinationVariable Long gameId, Message readyForNextRoundMessage) {
        Lobby currentLobby = lobbyService.getLobbyById(gameId);
        MessageType type = readyForNextRoundMessage.getType();
        if (type == MessageType.READY_FOR_NEXT_ROUND){
            UserGameStatus userGameStatus = objectMapper.convertValue(readyForNextRoundMessage.getPayload(), UserGameStatus.class);
            gameService.updateUserGameStatus(userGameStatus, currentLobby);
        }
    }
}
