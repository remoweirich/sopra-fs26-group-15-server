package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.objects.Admin;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;


@Controller
public class LobbyWebSocketController {

    private final LobbyService lobbyService;
    private final AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    public LobbyWebSocketController(LobbyService lobbyService, AuthService authService) {
        this.lobbyService = lobbyService;
        this.authService = authService;
    }

    @MessageMapping("/lobby/{lobbyId}/start")
    public void startGameAdmin(@DestinationVariable String lobbyId, Message message) {
        
        Lobby lobby = lobbyService.getLobbyById(Long.parseLong(lobbyId));

            
        // Authenticate the user
        // Convert payload to AuthHeader
        AuthHeader authHeader = objectMapper.convertValue(
            message.getPayload(), 
            AuthHeader.class
        );


        //Check whether user is admin of the lobby
        Long userId = authHeader.getUserId();
        if (!lobby.getAdmin().getUserId().equals(userId)) {
            return;
    }
        // Start the game
        lobbyService.startGame(Long.parseLong(lobbyId));
        
}
}
