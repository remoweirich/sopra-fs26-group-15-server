package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;
import ch.uzh.ifi.hase.soprafs26.objects.Game;

import ch.uzh.ifi.hase.soprafs26.entity.*;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;

import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import ch.uzh.ifi.hase.soprafs26.websocket.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

@Service
@Transactional
public class LobbyService {

    private final List<Lobby> activeLobbies = new ArrayList<>();
    private final UserService userService;
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyService(UserService userService, GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Lobby> getAllLobbies() {
        return activeLobbies;
    }

    public Lobby joinLobby(Long userId, Long lobbyId, String lobbyCode) {
        Lobby lobby = getLobbyById(lobbyId);
        
        User user = userService.getUserById(userId);

        //Check whether the lobby code is correct (only for private lobbies)
        if (lobby.getVisibility() == LobbyVisibility.PRIVATE && !Objects.equals(lobby.getLobbyCode(), lobbyCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect lobby code");
        }
        if (lobby.getUsers().stream().anyMatch(existingUser -> existingUser.getUserId().equals(userId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in this lobby");
        }
        // Check whether the lobby is full
        if (lobby.getUsers().size() >= lobby.getSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is full");
        }
        // add user to lobby
        lobby.addUser(user);

        //if Lobby is now full: if game is public, start game, else wait for admin to start the game
        if (lobby.getUsers().size() >= lobby.getSize() && lobby.getVisibility() == LobbyVisibility.PUBLIC) {
            lobby.setLobbyState(LobbyState.IN_GAME);
            startGame(lobby.getLobbyId());
        }

        //send broadcast message to lobby that user has joined
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);
        Message message = new Message(MessageType.LOBBY_STATE, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getLobbyId(), message);

        return lobby;
    }

    public void startGame(Long lobbyId) {

        Lobby lobby = getLobbyById(lobbyId);

        //create a Game object and fetch the Train data
        Game game = gameService.setupGame(lobby, 5);

        //update the Lobby object
        lobby.setGame(game);
        lobby.setLobbyState(LobbyState.IN_GAME);
    }

    public Lobby getLobbyById(Long lobbyId) {
        for (Lobby lobby : activeLobbies) {
            if (lobby.getLobbyId().equals(lobbyId)) {
                return lobby;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
    }

}
