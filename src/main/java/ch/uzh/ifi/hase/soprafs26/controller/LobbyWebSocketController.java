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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;


@Controller
public class LobbyWebSocketController {

    private final LobbyService lobbyService;
    private final AuthService authService;

    //@Autowired
    private final ObjectMapper objectMapper;

    public LobbyWebSocketController(LobbyService lobbyService, AuthService authService, ObjectMapper objectMapper) {
        this.lobbyService = lobbyService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @MessageMapping("/lobby/{lobbyId}/start")
    public void startGameAdmin(@DestinationVariable String lobbyId, Message message) {
        System.out.println("CHeck ob im controller");
        Lobby lobby = lobbyService.getLobbyById(Long.parseLong(lobbyId));

            
        // Authenticate the user
        // Convert payload to AuthHeader
//        AuthHeader authHeader = objectMapper.convertValue(
//            message.getPayload(),
//            AuthHeader.class
//        );

//        if (!authService.authUser(authHeader)) {
//            return;
//        }
//        System.out.println("Check nach auth check");
//
//        //Check whether user is admin of the lobby
//        Long userId = authHeader.getUserId();
//        if (!lobby.getAdmin().getUserId().equals(userId)) {
//            return;
//  }
        System.out.println("Check nach admin check");
        // Start the game
        lobbyService.startGame(Long.parseLong(lobbyId));
        
}

//Leave Lobby
@MessageMapping("/lobby/{lobbyId}/leave")
    public void leaveLobby(@DestinationVariable String lobbyId, Message message) {
        
        Lobby lobby = lobbyService.getLobbyById(Long.parseLong(lobbyId));

            
        // Authenticate the user
        // Convert payload to AuthHeader
        AuthHeader authHeader = objectMapper.convertValue(
            message.getPayload(), 
            AuthHeader.class
        );
        try{
            boolean isAuthenticated = authService.authUser(authHeader);

            if(!isAuthenticated){
                return;
            }
        
                // remove user from Lobby
                lobbyService.leaveLobby(Long.parseLong(lobbyId), authHeader.getUserId());
        }
        catch (ResponseStatusException e){
        }
                
}
}
