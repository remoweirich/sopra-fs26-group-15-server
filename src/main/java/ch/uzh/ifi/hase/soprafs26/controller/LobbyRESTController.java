package ch.uzh.ifi.hase.soprafs26.controller;

import ch.guessbb.sopraserver.constant.*;
import ch.uzh.ifi.hase.soprafs26.constant.LobbyVisibility;
import ch.uzh.ifi.hase.soprafs26.entity.Lobby;
import ch.guessbb.sopraserver.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;
import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;
import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
public class LobbyRESTController {

    public final LobbyService lobbyService;
    public final AuthService authService;

    public LobbyRESTController(LobbyService lobbyService, AuthService authService) {
        this.lobbyService = lobbyService;
        this.authService = authService;
    }

    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyAccessDTO createLobby(
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "userId", required = false) Long userId,
            @RequestBody CreateLobbyPostDTO createLobbyPostDTO) {

        boolean isGuest;
        LobbyAccessDTO lobbyAccessDTO;

        AuthHeader authHeader = new AuthHeader(userId, token);
        try {
            boolean isAuthenticated = authService.authUser(authHeader);
            if (!isAuthenticated) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            isGuest = false;
            lobbyAccessDTO = lobbyService.createLobby(createLobbyPostDTO, isGuest, userId, token);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                isGuest = true;
                lobbyAccessDTO = lobbyService.createLobby(createLobbyPostDTO, isGuest, null, null);
            } else {
                throw e;
            }
        }
        return lobbyAccessDTO;
    }


    @GetMapping("/lobbies")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<LobbyDTO> getAllLobbies() {
        List<Lobby> lobbies = lobbyService.getAllLobbies();

        List<LobbyDTO> lobbyDTOs = new ArrayList<>();

        for (Lobby lobby : lobbies) {
            LobbyDTO lobbyDTO = DTOMapper.INSTANCE.convertEntityToLobbyDTO(lobby);
            lobbyDTO.setCurrentPlayers(lobby.getPlayers().size());
            if (lobby.getVisibility() == LobbyVisibility.PRIVATE) {
                lobbyDTO.setLobbyCode("");
            }
            lobbyDTOs.add(lobbyDTO);
        }
        return lobbyDTOs;

    }

    @PostMapping("/lobbies/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public LobbyAccessDTO joinLobby(
            @PathVariable("id") Long lobbyId,
            @RequestHeader(value = "token", required = false) String token,
            @RequestHeader(value = "userId", required = false) Long userId,
            @RequestBody LobbyCodePostDTO lobbyCodePostDTO) {

        boolean isGuest;
        LobbyAccessDTO lobbyAccessDTO;

        AuthHeader authHeader = new AuthHeader(userId, token);
        try {
            boolean isAuthenticated = authService.authUser(authHeader);
            if (!isAuthenticated) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            isGuest = false;
            lobbyAccessDTO = lobbyService.joinLobby(userId, token, lobbyId, lobbyCodePostDTO.getLobbyCode(), isGuest);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                isGuest = true;
                lobbyAccessDTO = lobbyService.joinLobby(null, null, lobbyId, lobbyCodePostDTO.getLobbyCode(), isGuest);
            } else {
                throw e;
            }
        }
        return lobbyAccessDTO;
    }

    @GetMapping("/lobbies/{lobbyId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public MyLobbyDTO getMyLobby(
            @PathVariable("lobbyId") Long lobbyId,
            @RequestHeader("token") String token,
            @RequestHeader("userId") Long userId) {

        AuthHeader authHeader = new AuthHeader(userId, token);
        boolean isAuthenticated = authService.authUser(authHeader);
        if (!isAuthenticated) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        }
        Lobby lobby = lobbyService.getLobby(lobbyId, userId);
        MyLobbyDTO myLobbyDTO = DTOMapper.INSTANCE.convertEntityToMyLobbyDTO(lobby);
        myLobbyDTO.setCurrentPlayers(lobby.getPlayers().size());
        return myLobbyDTO;
    }

    @GetMapping("/game/{gameId}/leaderboard")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameResultDTO leaderboard(
            @PathVariable("gameId") Long gameId,
            @RequestHeader("token") String token,
            @RequestHeader("userId") Long userId) {

        AuthHeader authHeader = new AuthHeader(userId, token);
        if (!authService.authUser(authHeader)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        }

        return lobbyService.getGameResult(gameId);
    }
}
