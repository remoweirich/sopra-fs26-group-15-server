package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.objects.UserGameStatus;
import ch.uzh.ifi.hase.soprafs26.rest.dto.GuessMessageDTO;
import ch.uzh.ifi.hase.soprafs26.service.GameService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GameController {

    private final GameService gameService;
    private final LobbyService lobbyService;

    public GameController(GameService gameService, LobbyService lobbyService) {
        this.gameService = gameService;
        this.lobbyService = lobbyService;
    }

    @MessageMapping("/game/{gameId}/guess")
    public void processGuessMessage(
            @DestinationVariable Long gameId,
            @Header("userId") String userId,
            @Header("token") String token,
            GuessMessageDTO guessMessageDTO) {

        Lobby currentLobby = lobbyService.getLobbyById(gameId);
        gameService.processGuessMessage(guessMessageDTO, currentLobby);
    }

    @MessageMapping("/game/{gameId}/ready")
    public void readyForNextRound(
            @DestinationVariable Long gameId,
            @Header("userId") String userId,
            @Header("token") String token) {
        System.out.println("readyForNextRound called for game " + gameId + " by user " + userId);
        Lobby currentLobby = lobbyService.getLobbyById(gameId);
        UserGameStatus userGameStatus = new UserGameStatus(Long.parseLong(userId), true);
        gameService.readyForNextRound(userGameStatus, currentLobby);
    }
}