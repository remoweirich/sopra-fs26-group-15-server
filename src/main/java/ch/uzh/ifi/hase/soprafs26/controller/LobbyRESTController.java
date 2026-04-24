package ch.uzh.ifi.hase.soprafs26.controller;

import ch.uzh.ifi.hase.soprafs26.entity.GameResult;
import ch.uzh.ifi.hase.soprafs26.objects.Admin;
import ch.uzh.ifi.hase.soprafs26.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.*;

import ch.uzh.ifi.hase.soprafs26.constant.*;

import ch.uzh.ifi.hase.soprafs26.service.LobbyService;
import ch.uzh.ifi.hase.soprafs26.security.AuthHeader;
import ch.uzh.ifi.hase.soprafs26.security.AuthService;

import ch.uzh.ifi.hase.soprafs26.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs26.objects.Lobby;
import ch.uzh.ifi.hase.soprafs26.rest.dto.LobbyCodePostDTO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
public class LobbyRESTController {

    public final LobbyService lobbyService;
    public final AuthService authService;
    private final GameRepository gameRepository;

    public LobbyRESTController(LobbyService lobbyService, AuthService authService, GameRepository gameRepository) {
        this.lobbyService = lobbyService;
        this.authService = authService;
        this.gameRepository = gameRepository;
    }

    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyAccessDTO createLobby(@RequestHeader ("token") String token, @RequestHeader("userId") Long userId, @RequestBody CreateLobbyPostDTO createLobbyPostDTO){
        boolean isGuest;
        LobbyAccessDTO lobbyAccessDTO = null;

        AuthHeader authHeader = new AuthHeader(userId, token);
        try{
            boolean isAuthenticated = authService.authUser(authHeader);

            if(!isAuthenticated){
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            isGuest = false;
            lobbyAccessDTO = lobbyService.createLobby(createLobbyPostDTO, isGuest, userId, token);

        } catch (ResponseStatusException e){
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
            @RequestHeader("token") String token,
            @RequestHeader("userId") Long userId,
            @RequestBody LobbyCodePostDTO lobbyCodePostDTO) {

        boolean isGuest;
        LobbyAccessDTO lobbyAccessDTO = null;

        AuthHeader authHeader = new AuthHeader(userId, token);
        try{
            boolean isAuthenticated = authService.authUser(authHeader);

            if(!isAuthenticated){
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            isGuest = false;
            lobbyAccessDTO = lobbyService.joinLobby(userId, token, lobbyId, lobbyCodePostDTO.getLobbyCode(), isGuest);
        } catch (ResponseStatusException e){
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                isGuest = true;
                lobbyAccessDTO = lobbyService.joinLobby(null,null, lobbyId, lobbyCodePostDTO.getLobbyCode(), isGuest);

            } else {
                throw e;
            }

        }
        // 3. Mapping (Wichtig: lobbyAccessDTO darf nicht null sein!)
        //lobbyAccessDTO = DTOMapper.INSTANCE.convertLobbyToLobbyAccessDTO(lobby);

        return lobbyAccessDTO;    }

    @GetMapping("/lobbies/debug")
    @ResponseStatus(HttpStatus.OK)
        @ResponseBody
    public List<Lobby> getLobbiesDebug() {
        List<Lobby> lobbies = lobbyService.getAllLobbies();

        return lobbies;
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
        myLobbyDTO.setAdmin(new Admin(lobby.getAdmin().getUserId(), ""));
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
        boolean isAuthenticated = authService.authUser(authHeader);
        if (!isAuthenticated) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in");
        }

        GameResult gameResult = gameRepository.findByGameId(gameId);

        return DTOMapper.INSTANCE.convertGameResultToGameResultDTO(gameResult);
    }
}
