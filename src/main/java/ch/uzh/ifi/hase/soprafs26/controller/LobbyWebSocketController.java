package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;


@Controller
public class LobbyWebSocketController {

    private final LobbyService lobbyService;
    private final AuthService authService;

    public LobbyWebSocketController(LobbyService lobbyService, AuthService authService) {
        this.lobbyService = lobbyService;
        this.authService = authService;
    }

    @MessageMapping("/lobby/{lobbyId}/start")
    public void startGameAdmin(
            @DestinationVariable String lobbyId,
            @Header("userId") String userId,
            @Header("token") String token) {

        AuthHeader authHeader = new AuthHeader(Long.parseLong(userId), token);
        if (!authService.authUser(authHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Lobby lobby = lobbyService.getLobbyById(Long.parseLong(lobbyId));
        if (!lobby.getAdmin().getUserId().equals(Long.parseLong(userId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the admin can start the game");
        }

        lobbyService.startGame(Long.parseLong(lobbyId));
    }

    @MessageMapping("/lobby/{lobbyId}/leave")
    public void leaveLobby(
            @DestinationVariable String lobbyId,
            @Header("userId") String userId,
            @Header("token") String token) {

        AuthHeader authHeader = new AuthHeader(Long.parseLong(userId), token);
        if (!authService.authUser(authHeader)) {
            return;
        }

        lobbyService.leaveLobby(Long.parseLong(lobbyId), Long.parseLong(userId));
    }
}
