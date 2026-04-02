package ch.uzh.ifi.hase.soprafs26.controller;

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

    public LobbyRESTController(LobbyService lobbyService, AuthService authService) {
        this.lobbyService = lobbyService;
        this.authService = authService;
    }

    @PostMapping("/lobbies")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public LobbyAccessDTO createLobby(@RequestHeader ("token") String token, @RequestBody CreateLobbyPostDTO createLobbyPostDTO){
        boolean isGuest;
        LobbyAccessDTO lobbyAccessDTO = null;

        AuthHeader authHeader = new AuthHeader(createLobbyPostDTO.getUserId(), token);
        try{
            boolean isAuthenticated = authService.authUser(authHeader);

            if(!isAuthenticated){
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
            isGuest = false;
            lobbyAccessDTO = lobbyService.createLobby(createLobbyPostDTO, isGuest);

        } catch (ResponseStatusException e){
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                isGuest = true;
                lobbyAccessDTO = lobbyService.createLobby(createLobbyPostDTO, isGuest);

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
    public LobbyAccessDTO joinLobby(@PathVariable("id") Long lobbyId, @RequestHeader("Token") String token,
            @RequestHeader("UserId") Long userId,
            @RequestBody LobbyCodePostDTO lobbyCodePostDTO) {
        // in this version: lobbyCodePostDTO contains userID, modify based on
        // implementation of authService
        Lobby lobbyCodePostDTOentity = DTOMapper.INSTANCE.convertLobbyCodePostDTOtoEntity(lobbyCodePostDTO);
        // Check if the user is authenticated
        try {
            authService.authUser(new AuthHeader(userId, token));
            Lobby lobby = lobbyService.joinLobby(userId, lobbyId, lobbyCodePostDTOentity.getLobbyCode());
            // Return the lobby access information
            return DTOMapper.INSTANCE.convertEntityToLobbyAccessDTO(lobby);
        } catch (Exception e) {
            throw e;
        }
    }
}
