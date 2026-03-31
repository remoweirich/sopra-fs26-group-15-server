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
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
public class LobbyRESTController {

    public final LobbyService lobbyService;
    public final AuthService authService;

    LobbyRESTController(LobbyService lobbyService, AuthService authService) {
        this.lobbyService = lobbyService;
        this.authService = authService;
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
        authService.authUser(new AuthHeader(userId, token));
        Lobby lobby = lobbyService.joinLobby(userId, lobbyId, lobbyCodePostDTOentity.getLobbyCode());
        return DTOMapper.INSTANCE.convertEntityToLobbyAccessDTO(lobby);
    }
}
