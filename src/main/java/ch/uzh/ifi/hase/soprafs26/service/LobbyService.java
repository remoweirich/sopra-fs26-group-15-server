package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyState;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.*;

import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.MyLobbyDTO;

import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;

import ch.uzh.ifi.hase.soprafs26.constant.*;
import websocket.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Service
@Transactional
public class LobbyService {

    private List<Lobby> activeLobbies = new ArrayList<>();
    private final AuthService authService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public LobbyService(AuthService authService, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.authService = authService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Lobby> getAllLobbies() {
        return activeLobbies;
    }

    public Lobby joinLobby(String userId, String lobbyId, String lobbyCode) {
        Lobby lobby = getLobbyById(lobbyId);

        User user = userService.getUserById(userId);

        // Check whether the lobby code is correct (only for private lobbies)
        if (!lobby.getLobbyCode().equals(lobbyCode) && lobby.getVisibility() == LobbyVisibility.PRIVATE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Incorrect lobby code");
        }
        // Check whether the lobby is full
        if (lobby.getUsers().size() >= lobby.getSize()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lobby is full");
        }
        // add user to lobby
        lobby.addUser(user);

        // if Lobby is now full: if game is public, start game, else wait for admin to
        // start the game
        if (lobby.getUsers().size() >= lobby.getSize() && lobby.getVisibility() == LobbyVisibility.PUBLIC) {
            lobby.setLobbyState(LobbyState.IN_GAME);
            startGame(lobby.getLobbyId());
        }

        // send broadcast message to lobby that user has joined
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);
        Message message = new Message(MessageType.LOBBY_STATE, myLobbyDTO);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getLobbyId(), message);

        return lobby;
    }

    public void startGame(String lobbyId) {
    }

    private Lobby getLobbyById(String lobbyId) {
        for (Lobby lobby : activeLobbies) {
            if (lobby.getLobbyId().equals(lobbyId)) {
                return lobby;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
    }

    private Lobby getLobbyByCode(String lobbyCode) {
        for (Lobby lobby : activeLobbies) {
            if (lobby.getLobbyCode().equals(lobbyCode)) {
                return lobby;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found");
    }
}
